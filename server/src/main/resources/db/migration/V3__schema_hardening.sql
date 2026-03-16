-- Hardening Schema: FKs, Constraints, and Proper DataTypes

-- 1. Correcting Timestamp types from VARCHAR to BIGINT
ALTER TABLE categories ALTER COLUMN created_at TYPE BIGINT USING created_at::BIGINT;
ALTER TABLE menuitems ALTER COLUMN created_at TYPE BIGINT USING created_at::BIGINT;
ALTER TABLE itemvariants ADD COLUMN IF NOT EXISTS created_at BIGINT;
ALTER TABLE bills ALTER COLUMN created_at TYPE BIGINT USING created_at::BIGINT;
ALTER TABLE bills ALTER COLUMN paid_at TYPE BIGINT USING paid_at::BIGINT;
ALTER TABLE users ALTER COLUMN created_at TYPE BIGINT USING created_at::BIGINT;

-- 2. Scoping User uniqueness to Tenant
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
DROP INDEX IF EXISTS idx_users_email;
CREATE UNIQUE INDEX idx_users_tenant_email ON users (restaurant_id, email);

-- 3. Adding Missing Server FK Columns
ALTER TABLE menuitems ADD COLUMN server_category_id BIGINT;
ALTER TABLE itemvariants ADD COLUMN server_menu_item_id BIGINT;
ALTER TABLE bills ADD COLUMN last_reset_date VARCHAR(255); -- Used for daily order unique scoping

-- 4. Enforce NOT NULL on critical fields
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

-- 5. Foreign Key Constraints (DEFERRABLE to support client-side batch pushes)
ALTER TABLE menuitems
    ADD CONSTRAINT fk_menuitems_category
    FOREIGN KEY (server_category_id) REFERENCES categories(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE itemvariants
    ADD CONSTRAINT fk_itemvariants_menu_item
    FOREIGN KEY (server_menu_item_id) REFERENCES menuitems(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE bill_items
    ADD CONSTRAINT fk_bill_items_bill
    FOREIGN KEY (server_bill_id) REFERENCES bills(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE bill_items
    ADD CONSTRAINT fk_bill_items_menu_item
    FOREIGN KEY (server_menu_item_id) REFERENCES menuitems(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE bill_items
    ADD CONSTRAINT fk_bill_items_variant
    FOREIGN KEY (server_variant_id) REFERENCES itemvariants(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE bill_payments
    ADD CONSTRAINT fk_bill_payments_bill
    FOREIGN KEY (server_bill_id) REFERENCES bills(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE stock_logs
    ADD CONSTRAINT fk_stock_logs_menu_item
    FOREIGN KEY (server_menu_item_id) REFERENCES menuitems(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE stock_logs
    ADD CONSTRAINT fk_stock_logs_variant
    FOREIGN KEY (server_variant_id) REFERENCES itemvariants(id) DEFERRABLE INITIALLY DEFERRED;

-- 6. Bill Uniqueness Constraint (Restaurant ID + Daily ID + Date)
ALTER TABLE bills ADD CONSTRAINT uq_bills_daily_order
    UNIQUE (restaurant_id, daily_order_id, last_reset_date);

-- 7. Correcting RestaurantProfile last_reset_date type (if applicable)
-- Note: keeping it VARCHAR(255) in bills to match entity and print logic, 
-- but making it mandatory for unique constraint scoping.
