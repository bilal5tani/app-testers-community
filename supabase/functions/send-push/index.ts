// send-push: called by a Database Webhook on INSERT into public.notifications.
// Checks the user's notification preference, sends the push through FCM v1 to every
// device of the user, and removes tokens that Firebase says are dead.
//
// Secrets: FCM_SERVICE_ACCOUNT (service account JSON), WEBHOOK_SECRET (any long random string).
// Deploy:  supabase functions deploy send-push --no-verify-jwt
// Webhook: Dashboard > Database > Webhooks > table notifications, event Insert, Supabase Edge Function
//          send-push, add HTTP header  x-webhook-secret: <same WEBHOOK_SECRET>.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

// notification type -> preference column (null = always sent)
const PREF_FOR_TYPE: Record<string, string | null> = {
  tester_joined: "tester_joined",
  tester_left: "tester_joined",
  tester_missed: "tester_missed",
  reached_target: "reached_target",
  period_complete: "period_complete",
  new_apps: "new_apps",
  remind: "remind",
  streak_risk: "streak_risk",
  weekly: "weekly",
  nudge: null,
  removed: null,
  dropped: null,
};

// notification type -> Android channel (created in the app, plan section 7)
const CHANNEL_FOR_TYPE: Record<string, string> = {
  remind: "reminders",
  streak_risk: "reminders",
  nudge: "reminders",
  new_apps: "social",
  weekly: "social",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

// ---- Google OAuth for FCM v1 (service account -> access token), cached until it expires
let cachedToken: { value: string; expiresAt: number } | null = null;

const b64url = (data: ArrayBuffer | string) => {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : new Uint8Array(data);
  let s = "";
  bytes.forEach((b) => (s += String.fromCharCode(b)));
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
};

async function getAccessToken(sa: { client_email: string; private_key: string; token_uri?: string }) {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.expiresAt - 60 > now) return cachedToken.value;

  const tokenUri = sa.token_uri ?? "https://oauth2.googleapis.com/token";
  const header = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: tokenUri,
    iat: now,
    exp: now + 3600,
  }));
  const pem = sa.private_key.replace(/-----(BEGIN|END) PRIVATE KEY-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claim}`));
  const assertion = `${header}.${claim}.${b64url(sig)}`;

  const res = await fetch(tokenUri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  if (!res.ok) throw new Error(`oauth ${res.status}: ${await res.text()}`);
  const body = await res.json();
  cachedToken = { value: body.access_token, expiresAt: now + (body.expires_in ?? 3600) };
  return cachedToken.value;
}

Deno.serve(async (req) => {
  try {
    const secret = Deno.env.get("WEBHOOK_SECRET");
    if (!secret || req.headers.get("x-webhook-secret") !== secret) return json({ error: "unauthorized" }, 401);

    const payload = await req.json();
    const rec = payload?.record;
    if (!rec?.user_id || !rec?.type) return json({ error: "bad_payload" }, 400);

    const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

    // preference check (the inbox row already exists either way)
    const prefKey = rec.type in PREF_FOR_TYPE ? PREF_FOR_TYPE[rec.type] : null;
    if (prefKey) {
      const { data: prefs } = await admin.from("notification_prefs").select(prefKey).eq("user_id", rec.user_id).maybeSingle();
      if (prefs && (prefs as Record<string, unknown>)[prefKey] === false) return json({ status: "skipped_by_pref" });
    }

    const { data: devices } = await admin.from("devices").select("id, fcm_token").eq("user_id", rec.user_id);
    if (!devices?.length) return json({ status: "no_devices" });

    const saRaw = Deno.env.get("FCM_SERVICE_ACCOUNT");
    if (!saRaw) return json({ status: "fcm_not_configured" }, 500);
    const sa = JSON.parse(saRaw);
    const accessToken = await getAccessToken(sa);

    const data: Record<string, string> = {
      type: String(rec.type),
      notification_id: String(rec.id ?? ""),
      route: String(rec.data?.route ?? "notifications"),
    };

    let sent = 0;
    const dead: string[] = [];
    await Promise.all(devices.map(async (d: { id: string; fcm_token: string }) => {
      const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          message: {
            token: d.fcm_token,
            notification: { title: rec.title, body: rec.body },
            data,
            android: {
              priority: "HIGH",
              notification: { channel_id: CHANNEL_FOR_TYPE[rec.type] ?? "progress" },
            },
          },
        }),
      });
      if (res.ok) { sent++; return; }
      const err = await res.json().catch(() => ({}));
      const code = err?.error?.details?.find((x: { errorCode?: string }) => x.errorCode)?.errorCode;
      if (res.status === 404 || code === "UNREGISTERED" || code === "INVALID_ARGUMENT") dead.push(d.id);
      else console.error("fcm error", res.status, JSON.stringify(err));
    }));

    if (dead.length) await admin.from("devices").delete().in("id", dead);
    return json({ status: "ok", sent, removed_dead_tokens: dead.length });
  } catch (e) {
    console.error(e);
    return json({ error: (e as Error).message }, 500);
  }
});
