-- Unified Migration: BigDecimal + Schema Hardening + Safe Casting

-- 1. Convert Double (float8) columns to NUMERIC
ALTER TABLE bill_items
    ALTER COLUMN price         TYPE NUMERIC(12,2) USING price::NUMERIC,
    ALTER COLUMN item_total    TYPE NUMERIC(12,2) USING item_total::NUMERIC;

ALTER TABLE bills
    ALTER COLUMN subtotal          TYPE NUMERIC(12,2) USING subtotal::NUMERIC,
    ALTER COLUMN gst_percentage    TYPE NUMERIC(12,2) USING gst_percentage::NUMERIC,
    ALTER COLUMN cgst_amount       TYPE NUMERIC(12,2) USING cgst_amount::NUMERIC,
    ALTER COLUMN sgst_amount       TYPE NUMERIC(12,2) USING sgst_amount::NUMERIC,
    ALTER COLUMN custom_tax_amount TYPE NUMERIC(12,2) USING custom_tax_amount::NUMERIC,
    ALTER COLUMN total_amount      TYPE NUMERIC(12,2) USING total_amount::NUMERIC,
    ALTER COLUMN part_amount_1     TYPE NUMERIC(12,2) USING part_amount_1::NUMERIC,
    ALTER COLUMN part_amount_2     TYPE NUMERIC(12,2) USING part_amount_2::NUMERIC;

ALTER TABLE bill_payments
    ALTER COLUMN amount TYPE NUMERIC(12,2) USING amount::NUMERIC;

ALTER TABLE menuitems
    ALTER COLUMN base_price         TYPE NUMERIC(12,2) USING base_price::NUMERIC,
    ALTER COLUMN current_stock      TYPE NUMERIC(12,2) USING current_stock::NUMERIC,
    ALTER COLUMN low_stock_threshold TYPE NUMERIC(12,2) USING low_stock_threshold::NUMERIC;

ALTER TABLE itemvariants
    ALTER COLUMN price              TYPE NUMERIC(12,2) USING price::NUMERIC,
    ALTER COLUMN current_stock      TYPE NUMERIC(12,2) USING current_stock::NUMERIC,
    ALTER COLUMN low_stock_threshold TYPE NUMERIC(12,2) USING low_stock_threshold::NUMERIC;

ALTER TABLE restaurantprofiles
    ALTER COLUMN gst_percentage        TYPE NUMERIC(12,2) USING gst_percentage::NUMERIC,
    ALTER COLUMN custom_tax_percentage TYPE NUMERIC(12,2) USING custom_tax_percentage::NUMERIC;

ALTER TABLE stock_logs
    ALTER COLUMN delta TYPE NUMERIC(12,4) USING delta::NUMERIC;

-- 2. Correcting Timestamp types from VARCHAR to BIGINT with Safe Casting Logic
-- This handles existing string timestamps "yyyy-MM-dd HH:mm:ss", existing BIGINT strings, and NULLs/blanks.

-- Categories
ALTER TABLE categories ALTER COLUMN created_at TYPE BIGINT 
USING COALESCE(
    CASE 
        WHEN created_at ~ '^[0-9]+$' THEN created_at::BIGINT 
        WHEN created_at IS NOT NULL AND created_at <> '' THEN (EXTRACT(EPOCH FROM created_at::TIMESTAMP)::BIGINT * 1000)
    END, 0);

-- MenuItems
ALTER TABLE menuitems ALTER COLUMN created_at TYPE BIGINT 
USING COALESCE(
    CASE 
        WHEN created_at ~ '^[0-9]+$' THEN created_at::BIGINT 
        WHEN created_at IS NOT NULL AND created_at <> '' THEN (EXTRACT(EPOCH FROM created_at::TIMESTAMP)::BIGINT * 1000)
    END, 0);

-- Bills
ALTER TABLE bills ALTER COLUMN created_at TYPE BIGINT 
USING COALESCE(
    CASE 
        WHEN created_at ~ '^[0-9]+$' THEN created_at::BIGINT 
        WHEN created_at IS NOT NULL AND created_at <> '' THEN (EXTRACT(EPOCH FROM created_at::TIMESTAMP)::BIGINT * 1000)
    END, 0);

ALTER TABLE bills ALTER COLUMN paid_at TYPE BIGINT 
USING COALESCE(
    CASE 
        WHEN paid_at ~ '^[0-9]+$' THEN paid_at::BIGINT 
        WHEN paid_at IS NOT NULL AND paid_at <> '' THEN (EXTRACT(EPOCH FROM paid_at::TIMESTAMP)::BIGINT * 1000)
    END, 0);

-- Users
ALTER TABLE users ALTER COLUMN created_at TYPE BIGINT 
USING COALESCE(
    CASE 
        WHEN created_at ~ '^[0-9]+$' THEN created_at::BIGINT 
        WHEN created_at IS NOT NULL AND created_at <> '' THEN (EXTRACT(EPOCH FROM created_at::TIMESTAMP)::BIGINT * 1000)
    END, 0);

ALTER TABLE itemvariants ADD COLUMN IF NOT EXISTS created_at BIGINT;

-- 3. Scoping User uniqueness to Tenant
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
DROP INDEX IF EXISTS idx_users_email;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_tenant_email ON users (restaurant_id, email);

-- 4. Adding Missing Server FK Columns
ALTER TABLE menuitems ADD COLUMN IF NOT EXISTS server_category_id BIGINT;
ALTER TABLE itemvariants ADD COLUMN IF NOT EXISTS server_menu_item_id BIGINT;
ALTER TABLE bills ADD COLUMN IF NOT EXISTS last_reset_date VARCHAR(255);

-- 5. Enforce NOT NULL on critical fields
ALTER TABLE categories ALTER COLUMN name SET NOT NULL;
ALTER TABLE categories ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE menuitems ALTER COLUMN category_id SET NOT NULL;
ALTER TABLE menuitems ALTER COLUMN name SET NOT NULL;
ALTER TABLE menuitems ALTER COLUMN base_price SET NOT NULL;
ALTER TABLE menuitems ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE itemvariants ALTER COLUMN menu_item_id SET NOT NULL;
ALTER TABLE itemvariants ALTER COLUMN variant_name SET NOT NULL;
ALTER TABLE itemvariants ALTER COLUMN price SET NOT NULL;
ALTER TABLE itemvariants ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE bills ALTER COLUMN daily_order_id SET NOT NULL;
ALTER TABLE bills ALTER COLUMN lifetime_order_id SET NOT NULL;
ALTER TABLE bills ALTER COLUMN order_type SET NOT NULL;
ALTER TABLE bills ALTER COLUMN subtotal SET NOT NULL;
ALTER TABLE bills ALTER COLUMN total_amount SET NOT NULL;
ALTER TABLE bills ALTER COLUMN payment_mode SET NOT NULL;
ALTER TABLE bills ALTER COLUMN payment_status SET NOT NULL;
ALTER TABLE bills ALTER COLUMN order_status SET NOT NULL;
ALTER TABLE bills ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE bills ALTER COLUMN last_reset_date SET NOT NULL;

ALTER TABLE bill_items ALTER COLUMN bill_id SET NOT NULL;
ALTER TABLE bill_items ALTER COLUMN menu_item_id SET NOT NULL;
ALTER TABLE bill_items ALTER COLUMN item_name SET NOT NULL;
ALTER TABLE bill_items ALTER COLUMN price SET NOT NULL;
ALTER TABLE bill_items ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE bill_items ALTER COLUMN item_total SET NOT NULL;

ALTER TABLE bill_payments ALTER COLUMN bill_id SET NOT NULL;
ALTER TABLE bill_payments ALTER COLUMN payment_mode SET NOT NULL;
ALTER TABLE bill_payments ALTER COLUMN amount SET NOT NULL;

-- 6. Foreign Key Constraints (DEFERRABLE)
ALTER TABLE menuitems DROP CONSTRAINT IF EXISTS fk_menuitems_category;
ALTER TABLE menuitems
    ADD CONSTRAINT fk_menuitems_category
    FOREIGN KEY (server_category_id) REFERENCES categories(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE itemvariants DROP CONSTRAINT IF EXISTS fk_itemvariants_menu_item;
ALTER TABLE itemvariants
    ADD CONSTRAINT fk_itemvariants_menu_item
    FOREIGN KEY (server_menu_item_id) REFERENCES menuitems(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE bill_items DROP CONSTRAINT IF EXISTS fk_bill_items_bill;
ALTER TABLE bill_items
    ADD CONSTRAINT fk_bill_items_bill
    FOREIGN KEY (server_bill_id) REFERENCES bills(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE bill_items DROP CONSTRAINT IF EXISTS fk_bill_items_menu_item;
ALTER TABLE bill_items
    ADD CONSTRAINT fk_bill_items_menu_item
    FOREIGN KEY (server_menu_item_id) REFERENCES menuitems(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE bill_items DROP CONSTRAINT IF EXISTS fk_bill_items_variant;
ALTER TABLE bill_items
    ADD CONSTRAINT fk_bill_items_variant
    FOREIGN KEY (server_variant_id) REFERENCES itemvariants(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE bill_payments DROP CONSTRAINT IF EXISTS fk_bill_payments_bill;
ALTER TABLE bill_payments
    ADD CONSTRAINT fk_bill_payments_bill
    FOREIGN KEY (server_bill_id) REFERENCES bills(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE stock_logs DROP CONSTRAINT IF EXISTS fk_stock_logs_menu_item;
ALTER TABLE stock_logs
    ADD CONSTRAINT fk_stock_logs_menu_item
    FOREIGN KEY (server_menu_item_id) REFERENCES menuitems(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE stock_logs DROP CONSTRAINT IF EXISTS fk_stock_logs_variant;
ALTER TABLE stock_logs
    ADD CONSTRAINT fk_stock_logs_variant
    FOREIGN KEY (server_variant_id) REFERENCES itemvariants(id) DEFERRABLE INITIALLY DEFERRED;

-- 7. Bill Uniqueness Constraint (Restaurant ID + Daily ID + Date)
ALTER TABLE bills DROP CONSTRAINT IF EXISTS uq_bills_daily_order;
ALTER TABLE bills ADD CONSTRAINT uq_bills_daily_order
    UNIQUE (restaurant_id, daily_order_id, last_reset_date);
