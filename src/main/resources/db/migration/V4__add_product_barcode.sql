-- Barcode (EAN-13, UPC...) so products can be sold with a USB barcode scanner.
-- It is optional: several products may have no barcode (NULL), but two cannot share one.

ALTER TABLE products ADD COLUMN barcode VARCHAR(50);
CREATE UNIQUE INDEX ux_products_barcode ON products (barcode);
