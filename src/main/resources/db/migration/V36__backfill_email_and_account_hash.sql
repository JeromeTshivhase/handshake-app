-- Backfill email_hash and account_number_hash using pgcrypto digest (sha256)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Backfill email_hash for users where missing
UPDATE app_user
SET email_hash = encode(digest(lower(trim(email))::text, 'sha256'), 'hex')
WHERE email IS NOT NULL AND (email_hash IS NULL OR email_hash = '');

-- Backfill account_number_hash for payout methods where missing
UPDATE payout_method
SET account_number_hash = encode(digest(regexp_replace(account_number, '\\D', '', 'g')::text, 'sha256'), 'hex')
WHERE account_number IS NOT NULL AND (account_number_hash IS NULL OR account_number_hash = '');

