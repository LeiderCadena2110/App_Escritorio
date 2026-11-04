-- Rules the database itself enforces, even if a bug in the app tries to save bad data

ALTER TABLE products ADD CONSTRAINT ck_products_prices CHECK (cost_price >= 0 AND sale_price >= 0);
ALTER TABLE products ADD CONSTRAINT ck_products_min_stock CHECK (min_stock >= 0);

ALTER TABLE invoices ADD CONSTRAINT ck_invoices_amounts
    CHECK (subtotal >= 0 AND discount >= 0 AND discount <= subtotal AND tax >= 0 AND total >= 0 AND amount_paid >= 0);
ALTER TABLE invoices ADD CONSTRAINT ck_invoices_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 100);
ALTER TABLE invoices ADD CONSTRAINT ck_invoices_status CHECK (status IN ('PAID', 'CANCELLED'));
ALTER TABLE invoices ADD CONSTRAINT ck_invoices_payment_method
    CHECK (payment_method IN ('CASH', 'CARD', 'TRANSFER', 'CREDIT'));

ALTER TABLE invoice_items ADD CONSTRAINT ck_invoice_items_prices CHECK (unit_price >= 0 AND unit_cost >= 0);

ALTER TABLE stock_movements ADD CONSTRAINT ck_stock_movements_type CHECK (movement_type IN (
    'INITIAL', 'PURCHASE', 'CUSTOMER_RETURN', 'SALE', 'SALE_CANCELLATION', 'SUPPLIER_RETURN', 'DAMAGE', 'ADJUSTMENT'));
ALTER TABLE stock_movements ADD CONSTRAINT ck_stock_movements_quantity CHECK (quantity <> 0 AND stock_after >= 0);

ALTER TABLE customers ADD CONSTRAINT ck_customers_document_type
    CHECK (document_type IN ('CC', 'NIT', 'CE', 'TI', 'PP'));
