-- Create payout_method table and add email_hash to app_user
CREATE TABLE IF NOT EXISTS payout_method (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL UNIQUE REFERENCES app_user(id),
  bank_name VARCHAR(200),
  branch_code VARCHAR(50),
  account_number TEXT,
  account_number_hash VARCHAR(128),
  identity_fingerprint VARCHAR(128),
  created_at TIMESTAMPTZ DEFAULT now()
);

-- Add email_hash to app_user for blind-index lookups
ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS email_hash VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_payout_account_hash ON payout_method(account_number_hash);
CREATE INDEX IF NOT EXISTS idx_payout_user_id ON payout_method(user_id);

