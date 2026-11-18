package co.edu.ucc.stockflow.catalog.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.catalog.domain.Product;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProductCsvExportTest {

    @TempDir
    Path folder;

    @Test
    void writesProductsWithBomAndHeader() throws Exception {
        Product p = new Product();
        p.setSku("DEP-001");
        p.setName("Tenis; edición especial");
        p.setCostPrice(new BigDecimal("100"));
        p.setSalePrice(new BigDecimal("150"));
        p.setStock(2);
        Path file = folder.resolve("productos.csv");

        new ProductCsvExport().export(List.of(p), file);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertTrue(lines.get(0).startsWith("\uFEFFCódigo;Código de barras;Nombre;"));
        assertTrue(lines.get(1).startsWith("DEP-001;;\"Tenis; edición especial\";"));
        assertTrue(lines.get(1).contains(";200;") || lines.get(1).contains(";200,"), lines.get(1));
    }
}
