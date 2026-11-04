-- Indexes for the most common searches and reports

CREATE INDEX ix_invoices_status ON invoices (status, issued_at);
CREATE INDEX ix_invoices_customer ON invoices (customer_id);
CREATE INDEX ix_invoice_items_product ON invoice_items (product_id);
CREATE INDEX ix_products_category ON products (category_id);
CREATE INDEX ix_products_supplier ON products (supplier_id);
CREATE INDEX ix_stock_movements_created_at ON stock_movements (created_at);
