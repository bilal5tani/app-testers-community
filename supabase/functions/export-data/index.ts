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
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_ANON_KEY") ?? "",
      { global: { headers: { Authorization: req.headers.get("Authorization")! } } }
    );

    const { data: { user }, error: userError } = await supabaseClient.auth.getUser();
    if (userError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 401,
      });
    }

    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const [profile, profilePrivate, prefs, apps, enrollments, checkIns, feedback, notifications] = await Promise.all([
      supabaseAdmin.from("profiles").select("*").eq("id", user.id).single(),
      supabaseAdmin.from("profile_private").select("*").eq("user_id", user.id).single(),
      supabaseAdmin.from("notification_prefs").select("*").eq("user_id", user.id).single(),
      supabaseAdmin.from("apps").select("*").eq("owner_id", user.id),
      supabaseAdmin.from("app_testers").select("*").eq("tester_id", user.id),
      supabaseAdmin.from("check_ins").select("*").eq("tester_id", user.id),
      supabaseAdmin.from("feedback").select("*").eq("tester_id", user.id),
      supabaseAdmin.from("notifications").select("*").eq("user_id", user.id),
    ]);

    const exportPayload = {
      user_id: user.id,
      email: user.email,
      exported_at: new Date().toISOString(),
      profile: profile.data,
      private_info: profilePrivate.data,
      notification_preferences: prefs.data,
      my_apps: apps.data ?? [],
      my_enrollments: enrollments.data ?? [],
      my_check_ins: checkIns.data ?? [],
      my_feedback: feedback.data ?? [],
      notifications: notifications.data ?? [],
    };

    return new Response(JSON.stringify(exportPayload, null, 2), {
      headers: {
        ...corsHeaders,
        "Content-Type": "application/json",
        "Content-Disposition": `attachment; filename="app_testers_export_${user.id}.json"`,
      },
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});
