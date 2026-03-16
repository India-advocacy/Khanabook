-- Migration to convert Double (float8) columns to NUMERIC(12,2) for BigDecimal consistency

-- bill_items
ALTER TABLE bill_items
    ALTER COLUMN price         TYPE NUMERIC(12,2) USING price::NUMERIC,
    ALTER COLUMN item_total    TYPE NUMERIC(12,2) USING item_total::NUMERIC;

-- bills
ALTER TABLE bills
    ALTER COLUMN subtotal          TYPE NUMERIC(12,2) USING subtotal::NUMERIC,
    ALTER COLUMN gst_percentage    TYPE NUMERIC(12,2) USING gst_percentage::NUMERIC,
    ALTER COLUMN cgst_amount       TYPE NUMERIC(12,2) USING cgst_amount::NUMERIC,
    ALTER COLUMN sgst_amount       TYPE NUMERIC(12,2) USING sgst_amount::NUMERIC,
    ALTER COLUMN custom_tax_amount TYPE NUMERIC(12,2) USING custom_tax_amount::NUMERIC,
    ALTER COLUMN total_amount      TYPE NUMERIC(12,2) USING total_amount::NUMERIC,
    ALTER COLUMN part_amount_1     TYPE NUMERIC(12,2) USING part_amount_1::NUMERIC,
    ALTER COLUMN part_amount_2     TYPE NUMERIC(12,2) USING part_amount_2::NUMERIC;

-- bill_payments
ALTER TABLE bill_payments
    ALTER COLUMN amount TYPE NUMERIC(12,2) USING amount::NUMERIC;

-- menuitems
ALTER TABLE menuitems
    ALTER COLUMN base_price         TYPE NUMERIC(12,2) USING base_price::NUMERIC,
    ALTER COLUMN current_stock      TYPE NUMERIC(12,2) USING current_stock::NUMERIC,
    ALTER COLUMN low_stock_threshold TYPE NUMERIC(12,2) USING low_stock_threshold::NUMERIC;

-- itemvariants
ALTER TABLE itemvariants
    ALTER COLUMN price              TYPE NUMERIC(12,2) USING price::NUMERIC,
    ALTER COLUMN current_stock      TYPE NUMERIC(12,2) USING current_stock::NUMERIC,
    ALTER COLUMN low_stock_threshold TYPE NUMERIC(12,2) USING low_stock_threshold::NUMERIC;

-- restaurantprofiles
ALTER TABLE restaurantprofiles
    ALTER COLUMN gst_percentage        TYPE NUMERIC(12,2) USING gst_percentage::NUMERIC,
    ALTER COLUMN custom_tax_percentage TYPE NUMERIC(12,2) USING custom_tax_percentage::NUMERIC;

-- stock_logs (delta was Double, now NUMERIC(12,4) for weight precision)
ALTER TABLE stock_logs
    ALTER COLUMN delta TYPE NUMERIC(12,4) USING delta::NUMERIC;
