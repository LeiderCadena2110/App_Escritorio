package co.edu.ucc.stockflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.model.Product;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvExportServiceTest {

    @TempDir
    Path folder;

    @Test
    void quotesValuesWithSeparatorsOrQuotes() {
        assertEquals("simple", CsvExportService.escape("simple"));
        assertEquals("\"a;b\"", CsvExportService.escape("a;b"));
        assertEquals("\"dice \"\"hola\"\"\"", CsvExportService.escape("dice \"hola\""));
    }

    @Test
    void decimalsUseComma() {
        assertEquals("1234,50", CsvExportService.toText(new BigDecimal("1234.50")));
        assertEquals("", CsvExportService.toText(null));
    }

    @Test
    void writesProductsWithBomAndHeader() throws Exception {
        Product p = new Product();
        p.setSku("DEP-001");
        p.setName("Tenis; edición especial");
        p.setCostPrice(new BigDecimal("100"));
        p.setSalePrice(new BigDecimal("150"));
        p.setStock(2);
        Path file = folder.resolve("productos.csv");

        new CsvExportService().exportProducts(List.of(p), file);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertTrue(lines.get(0).startsWith("﻿Código;Nombre;"));
        assertTrue(lines.get(1).startsWith("DEP-001;\"Tenis; edición especial\";"));
        assertTrue(lines.get(1).contains(";200;") || lines.get(1).contains(";200,"), lines.get(1));
    }
}
