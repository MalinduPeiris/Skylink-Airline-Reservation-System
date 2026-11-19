# Promotion authorization migration (manual review and execution)

The approved model adds nullable `promotion.scope` (`GENERAL` or `TARGETED`) and nullable `promotion.active`. Null scope or null active is unavailable. Existing promotion IDs, codes, dates, and `promotion_users` links remain intact. `target_criteria` remains descriptive and is never used for authorization. New promotions are created with explicit scope but remain inactive until a Marketing Executive reviews and activates them.

## Before deployment

1. Schedule a maintenance window and verify database access with the database owner. Stop application writes.
2. Use a MySQL login path configured interactively outside this repository. Confirm the database name from `SKYLINK_DB_URL`, then back it up, for example `mysqldump --login-path=skylink-backup --single-transaction --routines --triggers --databases YOUR_DATABASE_NAME > C:\secure-backups\skylink-before-promotion.sql` (replace `YOUR_DATABASE_NAME` with that confirmed name). Keep the backup outside Git and verify that it can be restored in an isolated environment. Never place a password on a command line or in a tracked file.
3. Inventory the live schema and data before running DDL: `SHOW CREATE TABLE promotion;`, `SHOW CREATE TABLE promotion_users;`, `SELECT id, promo_code, validity_start, validity_end FROM promotion ORDER BY id;`, and `SELECT promotion_id, COUNT(*) FROM promotion_users GROUP BY promotion_id;`. Compare column names and types to the JPA mappings and review the result with the database owner.
4. Test the additive DDL against a restored copy. Confirm existing IDs, row counts, and linked-user counts are unchanged.

## Proposed additive DDL (review before executing)

```sql
ALTER TABLE promotion
  ADD COLUMN scope VARCHAR(16) NULL,
  ADD COLUMN active BOOLEAN NULL;
```

Do not set defaults or backfill either column. Do not delete or automatically classify legacy records. MySQL DDL may implicitly commit and can lock the table; plan downtime according to the live table size and MySQL version. The application now defaults to `spring.jpa.hibernate.ddl-auto=validate`, so it will not create these columns on startup. It will refuse to start against a database missing them. The `SKYLINK_JPA_DDL_AUTO` override is for isolated test databases and must not be set to `update` on a live database.

## Classification and verification

1. After migration, inspect `SELECT id, promo_code, scope, active FROM promotion ORDER BY id;`. All legacy rows should still have null scope and null active. Confirm original IDs and `promotion_users` counts.
2. Start the application with schema validation. A Marketing Executive reviews each promotion at `/api/marketing/promotion/{id}/eligibility`; an IT System Engineer can use `/dashboard/it-system-engineer/promotions/{id}/eligibility` under the existing staff authorization. Staff explicitly choose scope and eligible travelers, then activate it. For `GENERAL`, all linked selections must be cleared by staff. For active `TARGETED`, at least one existing traveler must be selected. Do not infer scope from `target_criteria` or existing links.
3. Verify an authenticated traveler can redeem an active `GENERAL` code and only a linked traveler can redeem an active `TARGETED` code. Check inactive, unclassified, future, and expired codes. Validity uses the `Asia/Colombo` calendar and includes the entire end date.
4. Verify booking totals against the server calculated fare and extras, then complete payment and finance review in a non-production environment using safe data.

If validation or checks fail, stop writes and investigate. The additive columns can remain unused while the previous application version is restored; do not drop columns or restore over live data without a separate approved recovery plan. Existing credentials in older Git history still require rotation and coordinated history remediation review before any feature branch push.
