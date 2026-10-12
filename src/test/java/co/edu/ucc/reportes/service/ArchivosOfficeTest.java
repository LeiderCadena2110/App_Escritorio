package co.edu.ucc.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Excel de entrada y Word de salida con Apache POI y poi-tl. */
class ArchivosOfficeTest {

    @TempDir
    Path tmp;

    @Test
    void leeExcelConFechasFormatosYFormulas() throws Exception {
        Path xlsx = tmp.resolve("ventas.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(xlsx)) {
            Sheet s = wb.createSheet("Ventas");
            wb.createSheet("Otra");
            CellStyle fecha = wb.createCellStyle();
            fecha.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle miles = wb.createCellStyle();
            miles.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("#,##0"));
            Row h = s.createRow(0);
            String[] titulos = {"Cliente", "Cédula", "Valor", "Fecha", "Doble"};
            for (int i = 0; i < titulos.length; i++) h.createCell(i).setCellValue(titulos[i]);
            Row r = s.createRow(1);
            r.createCell(0).setCellValue("Ana");
            r.createCell(1).setCellValue(1085123456d);
            r.createCell(2).setCellValue(1250000d);
            r.getCell(2).setCellStyle(miles);
            r.createCell(3).setCellValue(Date.from(LocalDate.of(2026, 9, 2).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            r.getCell(3).setCellStyle(fecha);
            r.createCell(4).setCellFormula("C2*2");
            r.getCell(4).setCellStyle(miles);
            wb.write(out);
        }

        assertEquals(List.of("Ventas", "Otra"), LectorExcel.hojas(xlsx));
        TablaDatos t = LectorExcel.leer(xlsx, "Ventas", FormatoValores.porDefecto());
        assertEquals("Ana", t.valor(0, "Cliente"));
        assertEquals("1085123456", t.valor(0, "Cédula"));
        assertEquals("1.250.000", t.valor(0, "Valor"));
        assertEquals("02/09/2026", t.valor(0, "Fecha"));
        assertEquals("2.500.000", t.valor(0, "Doble"));
    }

    @Test
    void detectaMarcadoresYGeneraWordPorFilaYEnTabla() throws Exception {
        Path plantilla = tmp.resolve("pedido.docx");
        try (XWPFDocument d = new XWPFDocument(); OutputStream out = Files.newOutputStream(plantilla)) {
            d.createParagraph().createRun().setText("Cliente: {{cliente}} – {{dirección}}");
            XWPFTable t = d.createTable(2, 2);
            t.getRow(0).getCell(0).setText("Producto{{filas}}");
            t.getRow(0).getCell(1).setText("Valor");
            t.getRow(1).getCell(0).setText("[producto]");
            t.getRow(1).getCell(1).setText("[valor]");
            d.write(out);
        }

        Marcadores m = DetectorMarcadores.detectarDocx(plantilla);
        assertEquals(List.of("cliente", "dirección"), m.simples());
        assertEquals(List.of("producto", "valor"), m.columnasTabla());
        assertTrue(m.tieneTabla());

        Map<String, Object> datos = new HashMap<>();
        datos.put("cliente", "Café Galeras");
        datos.put("dirección", "Calle 18 # 25-30");
        datos.put(DetectorMarcadores.TABLA, List.of(
                Map.of("producto", "Café de Nariño", "valor", "28.000"),
                Map.of("producto", "Panela", "valor", "6.500")));
        Path salida = tmp.resolve("salida.docx");
        GeneradorInforme.escribirDocx(plantilla, datos, salida);

        try (InputStream in = Files.newInputStream(salida);
             XWPFDocument d = new XWPFDocument(in);
             XWPFWordExtractor ex = new XWPFWordExtractor(d)) {
            String texto = ex.getText();
            assertTrue(texto.contains("Café Galeras"), texto);
            assertTrue(texto.contains("Calle 18 # 25-30"), texto);
            assertTrue(texto.contains("Café de Nariño") && texto.contains("Panela"), texto);
            assertFalse(texto.contains("{{") || texto.contains("[producto]"), texto);
            assertEquals(3, d.getTables().get(0).getNumberOfRows());
        }
    }
}
