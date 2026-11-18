package co.edu.ucc.stockflow.inventory.application;

import co.edu.ucc.stockflow.inventory.domain.StockMovement;
import co.edu.ucc.stockflow.shared.util.CsvWriter;
import co.edu.ucc.stockflow.shared.util.Formats;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/** Exports stock movements to a CSV file for Excel (columns in Spanish, as the user sees them). */
@Component
public class StockMovementCsvExport {

    public void export(List<StockMovement> movements, Path file) throws IOException {
        CsvWriter.write(file, movements, List.of(
                new CsvWriter.Column<StockMovement>("Fecha", m -> Formats.dateTime(m.getCreatedAt())),
                new CsvWriter.Column<StockMovement>("Código", StockMovement::getProductSku),
                new CsvWriter.Column<StockMovement>("Producto", StockMovement::getProductName),
                new CsvWriter.Column<StockMovement>("Tipo", m -> m.getType().label()),
                new CsvWriter.Column<StockMovement>("Cantidad", StockMovement::getQuantity),
                new CsvWriter.Column<StockMovement>("Stock resultante", StockMovement::getStockAfter),
                new CsvWriter.Column<StockMovement>("Costo unitario", StockMovement::getUnitCost),
                new CsvWriter.Column<StockMovement>("Referencia", StockMovement::getReference),
                new CsvWriter.Column<StockMovement>("Notas", StockMovement::getNotes)));
    }
}
