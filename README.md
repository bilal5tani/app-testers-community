# App Testers Community

A community platform connecting Android developers and closed-testers to fulfill Google Play's 12 testers for 14 days requirement.

## Tech Stack
- **Android App**: Kotlin, Jetpack Compose, Supabase-kt, Credential Manager, Coil, DataStore
- **Backend**: Supabase (Postgres, Row Level Security, RPC Functions, Edge Functions, pg_cron)

## Migration Workflow (N1)
To add database changes:
1. Create a new file in `supabase/migrations/NNNN_name.sql`.
2. Test locally using `npx supabase db push` or local Postgres.
3. Apply to hosted project with `npx supabase db push`.

## Key Hygiene & Security (N4)
- Only the `SUPABASE_ANON_KEY` is embedded in the Android app via `BuildConfig`.
- `SUPABASE_SERVICE_ROLE_KEY` is stored exclusively in Supabase Secrets for Edge Functions.

## Free-Tier Limits & Upgrade Trigger (N5)
- Database size limit: 500 MB
- File Storage limit: 1 GB
- **Upgrade Trigger**: Upgrade to Pro tier when database usage reaches 350 MB (70%) or active users exceed 35,000.

## Launch & Operational Notes (O1, O7)
- **Privacy Policy**: https://apptesters.community/privacy
- **Terms of Service**: https://apptesters.community/terms
- **Rollback Plan**:
  - In case of critical issue, pause sign-ups by setting `app_config.status = 'maintenance'`.
  - Restore database backup from Supabase dashboard.
