import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const payload = await req.json();
    const record = payload.record;
    if (!record || !record.user_id) {
      return new Response(JSON.stringify({ error: "Missing record" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 400,
      });
    }

    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const { data: prefs } = await supabaseAdmin
      .from("notification_prefs")
      .select("*")
      .eq("user_id", record.user_id)
      .single();

    const event = record.event;
    if (prefs && event in prefs && prefs[event] === false) {
      return new Response(JSON.stringify({ status: "skipped_by_pref" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const { data: devices } = await supabaseAdmin
      .from("devices")
      .select("id, fcm_token")
      .eq("user_id", record.user_id);

    if (!devices || devices.length === 0) {
      return new Response(JSON.stringify({ status: "no_devices" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const serviceAccountStr = Deno.env.get("FCM_SERVICE_ACCOUNT");
    if (!serviceAccountStr) {
      return new Response(JSON.stringify({ status: "fcm_not_configured" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    return new Response(JSON.stringify({ status: "success", device_count: devices.length }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});
