-- VAT per product (19 %, 5 % or 0 % exempt) instead of one rate for the whole invoice.
-- Existing products take the rate the business had configured (19 % if none).
-- Each invoice line stores the rate it was sold with, so old and new invoices can show the
-- tax broken down by rate. The invoice-level tax_rate becomes optional: it only has a value
-- when every line of the invoice used the same rate.

ALTER TABLE products ADD COLUMN tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 19;
UPDATE products SET tax_rate = COALESCE(
    (SELECT CAST(NULLIF(TRIM(setting_value), '') AS DECIMAL(5, 2)) FROM settings WHERE setting_key = 'invoice.taxRate'),
    19);
ALTER TABLE products ADD CONSTRAINT ck_products_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 100);

ALTER TABLE invoice_items ADD COLUMN tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 0;
UPDATE invoice_items SET tax_rate = (SELECT i.tax_rate FROM invoices i WHERE i.id = invoice_items.invoice_id);
ALTER TABLE invoice_items ADD CONSTRAINT ck_invoice_items_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 100);

ALTER TABLE invoices ALTER COLUMN tax_rate SET NULL;
