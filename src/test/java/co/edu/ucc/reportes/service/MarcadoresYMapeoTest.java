package co.edu.ucc.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.reportes.domain.PerfilMapeo;
import co.edu.ucc.reportes.domain.Plantilla;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarcadoresYMapeoTest {

    @Test
    void detectaMarcadoresYTablaDeFilas() {
        Marcadores m = DetectorMarcadores.detectarEnTextos(
                List.of("Señor {{cliente}}, de {{ ciudad }}", "Total: {{total}} {{cliente}}", "Producto{{filas}}", "[producto] [valor]"),
                List.of("Producto{{filas}}", "[producto]", "[valor]"));
        assertEquals(List.of("cliente", "ciudad", "total"), m.simples());
        assertEquals(List.of("producto", "valor"), m.columnasTabla());
        assertTrue(m.tieneTabla());
    }

    @Test
    void sinTablaNoTomaCorchetes() {
        Marcadores m = DetectorMarcadores.detectarEnTextos(List.of("Ver [anexo] de {{nombre}}"), List.of("[anexo]"));
        assertEquals(List.of("nombre"), m.simples());
        assertTrue(m.columnasTabla().isEmpty());
        assertFalse(m.tieneTabla());
    }

    @Test
    void detectaCamposDePlantillaHtml() {
        Marcadores m = DetectorMarcadores.detectarHtml("<p th:text=\"${fila['nombre']}\"></p><p th:text=\"${fila[ 'fecha' ]}\"></p>");
        assertEquals(List.of("nombre", "fecha"), m.simples());
    }

    @Test
    void sugiereColumnasParecidas() {
        Map<String, String> s = SugeridorMapeo.sugerir(
                List.of("nombre_cliente", "telefono", "total", "codigo_postal"),
                List.of("Nombre del cliente", "Teléfono", "Total a pagar", "Ciudad"));
        assertEquals("Nombre del cliente", s.get("nombre_cliente"));
        assertEquals("Teléfono", s.get("telefono"));
        assertEquals("Total a pagar", s.get("total"));
        assertNull(s.get("codigo_postal"));
    }

    @Test
    void faltantesYValoresEspeciales() {
        Plantilla p = new Plantilla();
        p.setListaMarcadores(List.of("cliente", "fecha"));
        p.setListaColumnasTabla(List.of("producto"));
        Map<String, String> mapeo = new LinkedHashMap<>();
        mapeo.put("cliente", "Cliente");
        assertEquals(List.of("fecha", "producto"), GeneradorInforme.faltantes(p, mapeo));

        TablaDatos t = new TablaDatos(null, null, "", List.of("Cliente"), List.of(List.of("Ana"), List.of("Luis")), List.of(2, 3), List.of());
        assertEquals("Luis", GeneradorInforme.valorDe(t, "Cliente", 1, "hoy"));
        assertEquals("2", GeneradorInforme.valorDe(t, GeneradorInforme.SEUDO_NUMERO, 1, "hoy"));
        assertEquals("2", GeneradorInforme.valorDe(t, GeneradorInforme.SEUDO_TOTAL, 0, "hoy"));
        assertEquals("hoy", GeneradorInforme.valorDe(t, GeneradorInforme.SEUDO_FECHA, 0, "hoy"));
    }

    @Test
    void perfilGuardaElMapeoConTildesYEspacios() {
        PerfilMapeo perfil = new PerfilMapeo();
        Map<String, String> m = new LinkedHashMap<>();
        m.put("dirección", "Dirección de envío");
        m.put("valor", "Valor = total");
        perfil.setMapeo(m);
        assertEquals(m, perfil.getMapeo());
    }

    @Test
    void nombresDeArchivoValidosEnLosTresSistemas() {
        assertEquals("Factura 12-2026", NombresArchivo.limpiar("Factura 12/2026", "x"));
        assertEquals("a b", NombresArchivo.limpiar("a:*?<>|b. ", "x"));
        assertEquals("x", NombresArchivo.limpiar("CON", "x"));
        assertEquals("x", NombresArchivo.limpiar("   ", "x"));
    }

    @Test
    void formatosDeNumeroYFecha() {
        FormatoValores f = FormatoValores.porDefecto();
        assertEquals("1085123456", f.numeroGeneral(1085123456d));
        assertEquals("12,5", f.numeroGeneral(12.5));
        assertEquals("1.234.567,89", f.numeroConMiles(1234567.89));
        assertEquals("15/03/2026", f.fecha(java.time.LocalDateTime.of(2026, 3, 15, 0, 0)));
        assertEquals("15/03/2026 14:30", f.fecha(java.time.LocalDateTime.of(2026, 3, 15, 14, 30)));
    }
}
