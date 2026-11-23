# Promotion authorization migration (manual review and execution)

The approved model adds nullable `promotion.scope` (`GENERAL` or `TARGETED`) and nullable `promotion.active`. Hibernate's MySQL mapping expects `scope VARCHAR(16)` and `active BIT(1)`. Null scope or null active is unavailable. Existing promotion IDs, codes, dates, and `promotion_users` links remain intact. `target_criteria` remains descriptive and is never used for authorization. New promotions are created with explicit scope but remain inactive until authorized staff review and activate them.

These names come from the JPA mappings. The live MySQL schema has not been inspected in this workspace: `SKYLINK_DB_*` variables and a MySQL client were unavailable at review time. A historical URL names `airline_db`, but that is only a candidate. Confirm the database name and live table definitions before running SQL. The older root-level `database-migration.sql` refers to `bookings` and `passengers`; it is unrelated to this migration and must not be run for promotion authorization.

## Before deployment

1. Schedule a maintenance window and verify database access with the database owner. Stop application writes.
2. Use the MySQL 8 command-line client with its interactive `--password` prompt. Never place a password on a command line, in PowerShell history, or in Git. Read the database name from the current `SKYLINK_DB_URL` and confirm it with `SELECT DATABASE(), VERSION(), CURRENT_USER()`.
3. Inventory the live schema and data before running DDL: `SHOW CREATE TABLE promotion;`, `SHOW CREATE TABLE promotion_users;`, `SHOW INDEX FROM promotion;`, `SHOW INDEX FROM promotion_users;`, and row counts for `promotion`, `promotion_users`, `booking`, and `payment`. Confirm both new columns are absent. Compare column names and types to the JPA mappings and review the result with the database owner.
4. Back up the entire database outside Git with `mysqldump --result-file=ABSOLUTE_PATH --single-transaction --quick --hex-blob --routines --triggers --events --no-tablespaces --skip-add-drop-table --set-gtid-purged=OFF DATABASE_NAME`. Supply host, port, user, and bare `--password` separately. Do **not** use PowerShell `>` or a text pipeline for the dump: Windows PowerShell can transcode output. Do **not** use `--databases`, which would embed the source database name and defeat a safe restore into a separate schema.
5. Require a zero dump exit code and nonempty file; record its SHA-256 hash. Restore it into a new, uniquely named disposable MySQL 8 database and compare row counts, IDs, and `promotion_users` links. The dump must have no `CREATE DATABASE`, `USE`, or `DROP TABLE` directives. Never import it into the original database as a test. Keep the backup access-restricted and outside Git.
6. Test the additive DDL against the restored copy first. Confirm original IDs, row counts, and linked-user counts are unchanged and the application starts with schema validation. Do not execute the live DDL until the database owner explicitly approves it.

## Windows PowerShell preparation (commands to run only when authorized)

These commands require MySQL 8 `mysql.exe` and `mysqldump.exe` on `PATH`. They stop if the tools or current URL are missing. The MySQL client prompts for each password without echoing it; no password value is placed in command history. Run in a maintenance window after confirming the source server and database.

```powershell
$skylinkMysql = (Get-Command mysql.exe -ErrorAction Stop).Source
$skylinkDump = (Get-Command mysqldump.exe -ErrorAction Stop).Source
$skylinkUrl = $env:SKYLINK_DB_URL
if ([string]::IsNullOrWhiteSpace($skylinkUrl) -or -not $skylinkUrl.StartsWith('jdbc:mysql://')) { throw 'Set SKYLINK_DB_URL to the current MySQL JDBC URL first.' }
$skylinkUri = [Uri]($skylinkUrl.Substring(5))
$skylinkDbName = [Uri]::UnescapeDataString($skylinkUri.AbsolutePath.Trim('/'))
if ($skylinkDbName -notmatch '^[A-Za-z0-9_]+$') { throw 'Review the JDBC database name manually.' }
$skylinkDbHost = $skylinkUri.Host
$skylinkDbPort = if ($skylinkUri.Port -gt 0) { $skylinkUri.Port } else { 3306 }
$skylinkBackupUser = Read-Host 'Backup-capable MySQL username'
& $skylinkMysql "--host=$skylinkDbHost" "--port=$skylinkDbPort" "--user=$skylinkBackupUser" '--password' "--database=$skylinkDbName" '--execute=SELECT DATABASE(), VERSION(), CURRENT_USER()'
if ($LASTEXITCODE -ne 0) { throw 'Database identity check failed.' }
$skylinkSourceServerUuid = & $skylinkMysql "--host=$skylinkDbHost" "--port=$skylinkDbPort" "--user=$skylinkBackupUser" '--password' '--batch' '--skip-column-names' '--execute=SELECT @@server_uuid'
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($skylinkSourceServerUuid)) { throw 'Source server identity check failed.' }
& $skylinkMysql "--host=$skylinkDbHost" "--port=$skylinkDbPort" "--user=$skylinkBackupUser" '--password' "--database=$skylinkDbName" '--execute=SHOW CREATE TABLE promotion; SHOW CREATE TABLE promotion_users; SHOW INDEX FROM promotion; SHOW INDEX FROM promotion_users; SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (''promotion'',''promotion_users'',''booking'',''payment''); SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ''promotion'' AND COLUMN_NAME IN (''scope'',''active'');'
if ($LASTEXITCODE -ne 0) { throw 'Live schema inspection failed.' }
& $skylinkMysql "--host=$skylinkDbHost" "--port=$skylinkDbPort" "--user=$skylinkBackupUser" '--password' "--database=$skylinkDbName" '--execute=SELECT COUNT(*) AS promotions, MIN(id) AS first_id, MAX(id) AS last_id, COALESCE(SUM(id),0) AS id_sum FROM promotion; SELECT COUNT(*) AS links FROM promotion_users; SELECT COUNT(*) AS bookings FROM booking; SELECT COUNT(*) AS payments FROM payment;'
if ($LASTEXITCODE -ne 0) { throw 'Source counts failed.' }
```

Confirm the output names the expected source database, MySQL 8, and InnoDB tables. If the schema differs, stop and revise this plan. The historical database-name candidate is not proof of the current source.

The following backup command reads the source but writes a new file outside this repository. `--result-file` preserves the dump bytes on Windows; `--skip-add-drop-table` and omission of `--databases` support a safer disposable restore. Do not redirect native output with `>` or pipe it through `Get-Content`.

```powershell
$skylinkBackupDir = Join-Path $env:USERPROFILE 'SkylinkBackups'
if ([IO.Path]::GetFullPath($skylinkBackupDir).StartsWith((Resolve-Path '.').Path + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Backup directory is inside the Git repository.' }
New-Item -ItemType Directory -Path $skylinkBackupDir -Force | Out-Null
$skylinkBackupFile = Join-Path $skylinkBackupDir ('skylink-' + $skylinkDbName + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8) + '.sql')
if (Test-Path -LiteralPath $skylinkBackupFile) { throw 'Refusing to overwrite a backup.' }
& $skylinkDump "--host=$skylinkDbHost" "--port=$skylinkDbPort" "--user=$skylinkBackupUser" '--password' '--single-transaction' '--quick' '--hex-blob' '--routines' '--triggers' '--events' '--no-tablespaces' '--skip-add-drop-table' '--set-gtid-purged=OFF' '--default-character-set=utf8mb4' "--result-file=$skylinkBackupFile" $skylinkDbName
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $skylinkBackupFile)) { throw 'Backup failed; do not migrate.' }
if ((Get-Item -LiteralPath $skylinkBackupFile).Length -eq 0) { throw 'Backup file is empty; do not migrate.' }
if (Select-String -LiteralPath $skylinkBackupFile -Pattern '^\s*(CREATE DATABASE|USE\s+|DROP DATABASE|DROP TABLE)' -Quiet) { throw 'Dump contains database-switch or destructive table directives; review before restore.' }
Get-FileHash -LiteralPath $skylinkBackupFile -Algorithm SHA256 | Select-Object Algorithm, Hash, Path
```

For the restore test, require a separate disposable MySQL 8 instance with the event scheduler disabled and no network access back to the source. Restored triggers or events may contain schema-qualified references, so a new database on the original server is not enough isolation. The commands below compare server UUIDs and refuse to create a database on the source instance. Run them only when creating a disposable database is approved. The `source` command is a MySQL client command, not PowerShell redirection. Use a backup path without spaces for this command.

```powershell
$skylinkRestoreHost = Read-Host 'Disposable MySQL 8 host (confirm intentionally)'
$skylinkRestorePort = [int](Read-Host 'Disposable MySQL 8 port')
$skylinkRestoreUser = Read-Host 'MySQL account authorized to create a disposable database'
$skylinkRestoreDb = 'skylink_restore_' + [guid]::NewGuid().ToString('N').Substring(0,12)
$skylinkSourceFile = $skylinkBackupFile.Replace('\', '/')
if ($skylinkSourceFile.Contains(' ')) { throw 'Use a backup path without spaces for the mysql source command.' }
$skylinkRestoreServerUuid = & $skylinkMysql "--host=$skylinkRestoreHost" "--port=$skylinkRestorePort" "--user=$skylinkRestoreUser" '--password' '--batch' '--skip-column-names' '--execute=SELECT @@server_uuid'
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($skylinkRestoreServerUuid) -or $skylinkRestoreServerUuid -eq $skylinkSourceServerUuid) { throw 'Restore target is unavailable or is the source instance.' }
$skylinkEventScheduler = & $skylinkMysql "--host=$skylinkRestoreHost" "--port=$skylinkRestorePort" "--user=$skylinkRestoreUser" '--password' '--batch' '--skip-column-names' '--execute=SELECT @@event_scheduler'
if ($LASTEXITCODE -ne 0 -or $skylinkEventScheduler -eq 'ON') { throw 'Disable the event scheduler on the disposable instance before restoring.' }
& $skylinkMysql "--host=$skylinkRestoreHost" "--port=$skylinkRestorePort" "--user=$skylinkRestoreUser" '--password' "--execute=CREATE DATABASE $skylinkRestoreDb CHARACTER SET utf8mb4"
if ($LASTEXITCODE -ne 0) { throw 'Disposable database creation failed; stop.' }
& $skylinkMysql "--host=$skylinkRestoreHost" "--port=$skylinkRestorePort" "--user=$skylinkRestoreUser" '--password' "--database=$skylinkRestoreDb" "--execute=source $skylinkSourceFile"
if ($LASTEXITCODE -ne 0) { throw 'Disposable restore failed; stop.' }
& $skylinkMysql "--host=$skylinkRestoreHost" "--port=$skylinkRestorePort" "--user=$skylinkRestoreUser" '--password' "--database=$skylinkRestoreDb" '--execute=SELECT COUNT(*) AS promotions, MIN(id) AS first_id, MAX(id) AS last_id, COALESCE(SUM(id),0) AS id_sum FROM promotion; SELECT COUNT(*) AS links FROM promotion_users; SELECT COUNT(*) AS bookings FROM booking; SELECT COUNT(*) AS payments FROM payment;'
if ($LASTEXITCODE -ne 0) { throw 'Restored data verification failed; stop.' }
```

Compare these restored counts and IDs with read-only counts from the source before migration. Do not automatically drop the disposable database; retain it for review or arrange separate cleanup approval. If routines or definers prevent a clean restore, stop and ask the database owner to resolve the backup rather than editing the dump in place.

## Credential rotation and application environment

The database username in older tracked configuration was `root`. That historical password must be treated as exposed, even though the current tracked configuration contains only placeholders. Confirm the server and exact MySQL account identity (`CURRENT_USER()` includes the host) before rotating it. Inventory other applications using that account and coordinate a maintenance window; rotating a shared root account without that review can break them. Prefer a dedicated Skylink application account limited to the confirmed database and required runtime permissions. Use a separate privileged account for this manual DDL and backups. Change account passwords through MySQL Workbench's masked account-management fields or another approved interactive administrative tool; do not put `IDENTIFIED BY 'password'` into PowerShell or MySQL history. Retire or rotate the exposed root credential only after dependent applications are accounted for. Review historical-secret remediation with the team; do not rewrite shared Git history or force push.

Set the application variables only for the current PowerShell session. The JDBC URL must contain host, port, and database name but no embedded credentials. The password prompt is masked, and no value is written to a file or printed. Do not run `Get-ChildItem Env:` afterward because it would display the process environment. Close the shell to clear the session values.

```powershell
$env:SKYLINK_DB_URL = Read-Host 'Current Skylink MySQL JDBC URL'
$env:SKYLINK_DB_USERNAME = Read-Host 'Dedicated Skylink MySQL username'
$skylinkSecurePassword = Read-Host 'Skylink MySQL password' -AsSecureString
$env:SKYLINK_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $skylinkSecurePassword).Password
Remove-Variable skylinkSecurePassword
Remove-Item Env:SKYLINK_JPA_DDL_AUTO -ErrorAction SilentlyContinue
```

The last command removes any test-only DDL override from this shell so the application uses `validate`. Never persist the password with `setx` or in `application.yml`.

## Proposed additive DDL (review before executing)

```sql
ALTER TABLE `promotion`
  ADD COLUMN `scope` VARCHAR(16) NULL,
  ADD COLUMN `active` BIT(1) NULL,
  ALGORITHM=INSTANT,
  LOCK=DEFAULT;
```

Run the statement only after the live table and MySQL 8 minor version are confirmed. `ALGORITHM=INSTANT` is available for eligible `ADD COLUMN` operations on MySQL 8.0.12+. MySQL permits only `LOCK=DEFAULT` with `ALGORITHM=INSTANT`; if this statement fails, stop for a DBA review instead of silently retrying with a table-copy algorithm. It can still wait briefly for a metadata lock. MySQL DDL implicitly commits, so there is no transaction rollback. Do not set defaults or backfill either column. Do not delete or automatically classify legacy records. The application defaults to `spring.jpa.hibernate.ddl-auto=validate`, so it will refuse to start against a database missing these columns and will not add them on startup. Never set `SKYLINK_JPA_DDL_AUTO=update` on a live database.

## Classification and verification

1. After migration, inspect `SHOW CREATE TABLE promotion;`, `SHOW INDEX FROM promotion;`, and `SHOW INDEX FROM promotion_users;`. Both new columns must be nullable and existing keys and indexes unchanged. Before staff classification, the `untouched_legacy` count below must equal `total` and `unexpectedly_classified` must be zero. Compare promotion ID counts/min/max/sum and `promotion_users`, `booking`, and `payment` counts with the pre-migration inventory and restored copy.
2. Start the application with schema validation. A Marketing Executive reviews each promotion at `/api/marketing/promotion/{id}/eligibility`; an IT System Engineer can use `/dashboard/it-system-engineer/promotions/{id}/eligibility` under the existing staff authorization. Staff explicitly choose scope and eligible travelers, then activate it. For `GENERAL`, all linked selections must be cleared by staff. For active `TARGETED`, at least one existing traveler must be selected. Do not infer scope from `target_criteria` or existing links.
3. Verify an authenticated traveler can redeem an active `GENERAL` code and only a linked traveler can redeem an active `TARGETED` code. Check inactive, unclassified, future, and expired codes. Validity uses the `Asia/Colombo` calendar and includes the entire end date.
4. Verify booking totals against the server calculated fare and extras, then complete payment and finance review in a non-production environment using safe data.

```sql
SHOW CREATE TABLE `promotion`;
SHOW INDEX FROM `promotion`;
SHOW INDEX FROM `promotion_users`;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion'
  AND COLUMN_NAME IN ('scope', 'active');
SELECT COUNT(*) AS total,
       COALESCE(SUM(scope IS NULL AND active IS NULL), 0) AS untouched_legacy,
       COALESCE(SUM(scope IS NOT NULL OR active IS NOT NULL), 0) AS unexpectedly_classified,
       MIN(id) AS first_id, MAX(id) AS last_id, COALESCE(SUM(id), 0) AS id_sum
FROM `promotion`;
SELECT COUNT(*) AS links FROM `promotion_users`;
SELECT COUNT(*) AS bookings FROM `booking`;
SELECT COUNT(*) AS payments FROM `payment`;
```

If validation or checks fail, stop application writes and investigate with the database owner. The additive columns can remain in place; do not drop them or import the backup over live data as an automatic rollback. Reverting to earlier application code would reintroduce promotion authorization weaknesses and could re-enable `ddl-auto=update`; it requires a separate security and configuration review. Use the verified disposable restore and source backup to plan any recovery, and require explicit approval before altering live data. Rotation of the historical root credential remains a prerequisite for any feature branch push.

MySQL references: [mysqldump options and Windows `--result-file`](https://dev.mysql.com/doc/refman/8.0/en/mysqldump.html), [online `ADD COLUMN` support](https://dev.mysql.com/doc/refman/8.0/en/innodb-online-ddl-operations.html), [the `ALTER TABLE` lock rules](https://dev.mysql.com/doc/refman/8.0/en/alter-table.html), and [running a script with `mysql source`](https://dev.mysql.com/doc/refman/8.0/en/batch-mode.html).
