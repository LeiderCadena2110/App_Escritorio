package co.edu.ucc.stockflow.catalog.application;

import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.shared.util.CsvWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/** Exports products to a CSV file for Excel (columns in Spanish, as the user sees them). */
@Component
public class ProductCsvExport {

    public void export(List<Product> products, Path file) throws IOException {
        CsvWriter.write(file, products, List.of(
                new CsvWriter.Column<Product>("Código", Product::getSku),
                new CsvWriter.Column<Product>("Código de barras", Product::getBarcode),
                new CsvWriter.Column<Product>("Nombre", Product::getName),
                new CsvWriter.Column<Product>("Categoría", Product::getCategoryName),
                new CsvWriter.Column<Product>("Proveedor", Product::getSupplierName),
                new CsvWriter.Column<Product>("Unidad", Product::getUnit),
                new CsvWriter.Column<Product>("Costo", Product::getCostPrice),
                new CsvWriter.Column<Product>("Precio de venta (sin IVA)", Product::getSalePrice),
                new CsvWriter.Column<Product>("IVA %", Product::getTaxRate),
                new CsvWriter.Column<Product>("Margen %", Product::getMarginPercent),
                new CsvWriter.Column<Product>("Stock", Product::getStock),
                new CsvWriter.Column<Product>("Stock mínimo", Product::getMinStock),
                new CsvWriter.Column<Product>("Valor en inventario", Product::getStockValue),
                new CsvWriter.Column<Product>("Activo", p -> p.isActive() ? "Sí" : "No")));
    }
}
