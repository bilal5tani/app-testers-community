// delete-account: signed-in user deletes their own account.
// Removes the user's Storage files (including nested feedback/<uid>/<app_id>/ folders),
// then deletes the auth user; every table row follows through ON DELETE CASCADE.
// Deploy: supabase functions deploy delete-account   (JWT verification stays ON)
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...cors, "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const auth = req.headers.get("Authorization");
    if (!auth) return json({ error: "unauthorized" }, 401);

    const asUser = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
      global: { headers: { Authorization: auth } },
    });
    const { data: { user }, error } = await asUser.auth.getUser();
    if (error || !user) return json({ error: "unauthorized" }, 401);

    const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

    // collect every file under <bucket>/<uid>/ (folders have no id), then remove in one call per bucket
    const collect = async (bucket: string, prefix: string): Promise<string[]> => {
      const { data } = await admin.storage.from(bucket).list(prefix, { limit: 1000 });
      const out: string[] = [];
      for (const item of data ?? []) {
        const path = `${prefix}/${item.name}`;
        if (item.id) out.push(path);
        else out.push(...await collect(bucket, path));
      }
      return out;
    };
    for (const bucket of ["avatars", "app-icons", "feedback"]) {
      const files = await collect(bucket, user.id);
      if (files.length) await admin.storage.from(bucket).remove(files);
    }

    const { error: delErr } = await admin.auth.admin.deleteUser(user.id);
    if (delErr) throw delErr;
    return json({ status: "account_deleted" });
  } catch (e) {
    return json({ error: (e as Error).message }, 500);
  }
});
