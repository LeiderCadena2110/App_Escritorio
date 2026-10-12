package co.edu.ucc.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LectorCsvTest {

    @TempDir
    Path tmp;

    @Test
    void detectaPuntoYComaComoLoGuardaExcelEnEspanol() {
        assertEquals(';', LectorCsv.detectarSeparador("Nombre;Valor;Fecha\nAna;1.234,5;01/02/2026\nLuis;20,0;02/02/2026\n"));
    }

    @Test
    void detectaComaYNoCuentaLasComasEntreComillas() {
        assertEquals(',', LectorCsv.detectarSeparador("a,b,c\n\"x; y\",2,3\n\"z; w\",4,5\n"));
    }

    @Test
    void detectaTabulador() {
        assertEquals('\t', LectorCsv.detectarSeparador("a\tb\tc\n1\t2\t3\n"));
    }

    @Test
    void detectaCodificacion() {
        assertEquals(StandardCharsets.UTF_8, LectorCsv.detectarCodificacion("Muñoz".getBytes(StandardCharsets.UTF_8)));
        assertEquals(LectorCsv.WINDOWS_1252, LectorCsv.detectarCodificacion("Muñoz".getBytes(LectorCsv.WINDOWS_1252)));
    }

    @Test
    void leeCsvAnsiConEncabezadosVaciosYRepetidos() throws Exception {
        Path f = tmp.resolve("datos.csv");
        String texto = "Nombre;Ciudad;;Ciudad\r\n\r\nMaría;Pasto;x;Ipiales\r\nJosé;Túquerres;y;Tumaco\r\n";
        Files.write(f, texto.getBytes(LectorCsv.WINDOWS_1252));

        TablaDatos t = LectorCsv.leer(f);

        assertEquals(List.of("Nombre", "Ciudad", "Columna 3", "Ciudad (2)"), t.columnas());
        assertEquals(2, t.totalFilas());
        assertEquals("María", t.valor(0, "Nombre"));
        assertEquals("Túquerres", t.valor(1, "Ciudad"));
        // La línea vacía se salta, pero los números de fila siguen siendo los del archivo
        assertEquals(List.of(3, 4), t.numerosFila());
        assertEquals(2, t.problemasLectura().size());
    }

    @Test
    void archivoSoloConEncabezadoEsError() throws Exception {
        Path f = tmp.resolve("vacio.csv");
        Files.writeString(f, "a,b\n");
        TablaDatos t = LectorCsv.leer(f);
        assertTrue(ValidadorDatos.hayErrores(ValidadorDatos.validar(t)));
    }
}
