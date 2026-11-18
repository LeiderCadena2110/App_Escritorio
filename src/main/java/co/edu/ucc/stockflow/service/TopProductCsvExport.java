package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.dto.TopProduct;
import co.edu.ucc.stockflow.util.CsvWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/** Exports best-selling products to a CSV file for Excel (columns in Spanish, as the user sees them). */
@Component
public class TopProductCsvExport {

    public void export(List<TopProduct> top, Path file) throws IOException {
        CsvWriter.write(file, top, List.of(
                new CsvWriter.Column<TopProduct>("Código", TopProduct::sku),
                new CsvWriter.Column<TopProduct>("Producto", TopProduct::name),
                new CsvWriter.Column<TopProduct>("Unidades vendidas", TopProduct::quantity),
                new CsvWriter.Column<TopProduct>("Ventas", TopProduct::revenue),
                new CsvWriter.Column<TopProduct>("Ganancia", TopProduct::profit)));
    }
}
