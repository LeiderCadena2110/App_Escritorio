package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.dto.TopProduct;
import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.model.StockMovement;
import co.edu.ucc.stockflow.util.Formats;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * Exports tables to CSV that Excel opens directly: UTF-8 with BOM (for accents), semicolon as
 * separator and comma as decimal separator, as Excel expects in Spanish.
 */
@Service
public class CsvExportService {

    private static final char SEPARATOR = ';';

    /** A CSV column: its title and how to get the value from a row. */
    public record Column<T>(String title, Function<T, Object> value) {
    }

    public void exportProducts(List<Product> products, Path file) throws IOException {
        write(file, products, List.of(
                new Column<Product>("Código", Product::getSku),
                new Column<Product>("Nombre", Product::getName),
                new Column<Product>("Categoría", Product::getCategoryName),
                new Column<Product>("Proveedor", Product::getSupplierName),
                new Column<Product>("Unidad", Product::getUnit),
                new Column<Product>("Costo", Product::getCostPrice),
                new Column<Product>("Precio de venta", Product::getSalePrice),
                new Column<Product>("Margen %", Product::getMarginPercent),
                new Column<Product>("Stock", Product::getStock),
                new Column<Product>("Stock mínimo", Product::getMinStock),
                new Column<Product>("Valor en inventario", Product::getStockValue),
                new Column<Product>("Activo", p -> p.isActive() ? "Sí" : "No")));
    }

    public void exportInvoices(List<Invoice> invoices, Path file) throws IOException {
        write(file, invoices, List.of(
                new Column<Invoice>("Número", Invoice::getNumber),
                new Column<Invoice>("Fecha", i -> Formats.dateTime(i.getIssuedAt())),
                new Column<Invoice>("Cliente", Invoice::getCustomerName),
                new Column<Invoice>("Documento", Invoice::getCustomerDocument),
                new Column<Invoice>("Subtotal", Invoice::getSubtotal),
                new Column<Invoice>("Descuento", Invoice::getDiscount),
                new Column<Invoice>("IVA", Invoice::getTax),
                new Column<Invoice>("Total", Invoice::getTotal),
                new Column<Invoice>("Forma de pago", i -> i.getPaymentMethod().label()),
                new Column<Invoice>("Estado", i -> i.getStatus().label())));
    }

    public void exportMovements(List<StockMovement> movements, Path file) throws IOException {
        write(file, movements, List.of(
                new Column<StockMovement>("Fecha", m -> Formats.dateTime(m.getCreatedAt())),
                new Column<StockMovement>("Código", StockMovement::getProductSku),
                new Column<StockMovement>("Producto", StockMovement::getProductName),
                new Column<StockMovement>("Tipo", m -> m.getType().label()),
                new Column<StockMovement>("Cantidad", StockMovement::getQuantity),
                new Column<StockMovement>("Stock resultante", StockMovement::getStockAfter),
                new Column<StockMovement>("Costo unitario", StockMovement::getUnitCost),
                new Column<StockMovement>("Referencia", StockMovement::getReference),
                new Column<StockMovement>("Notas", StockMovement::getNotes)));
    }

    public void exportTopProducts(List<TopProduct> top, Path file) throws IOException {
        write(file, top, List.of(
                new Column<TopProduct>("Código", TopProduct::sku),
                new Column<TopProduct>("Producto", TopProduct::name),
                new Column<TopProduct>("Unidades vendidas", TopProduct::quantity),
                new Column<TopProduct>("Ventas", TopProduct::revenue),
                new Column<TopProduct>("Ganancia", TopProduct::profit)));
    }

    public <T> void write(Path file, List<T> rows, List<Column<T>> columns) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('﻿');
            List<String> titles = new ArrayList<>();
            for (Column<T> c : columns) titles.add(c.title());
            writeLine(w, titles);
            for (T row : rows) {
                List<String> values = new ArrayList<>();
                for (Column<T> c : columns) values.add(toText(c.value().apply(row)));
                writeLine(w, values);
            }
        }
    }

    static String toText(Object value) {
        if (value == null) return "";
        if (value instanceof BigDecimal d) return d.toPlainString().replace('.', ',');
        return value.toString();
    }

    /** Quotes the value when it has the separator, quotes or line breaks. */
    static String escape(String value) {
        if (value.indexOf(SEPARATOR) >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static void writeLine(Writer w, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) w.write(SEPARATOR);
            w.write(escape(values.get(i)));
        }
        w.write("\r\n");
    }
}
