package co.edu.ucc.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Las plantillas HTML de la app se llenan con Thymeleaf y salen como PDF válido. */
class PlantillasPdfTest {

    @TempDir
    Path tmp;

    private final MotorPdf motor = new MotorPdf();

    @Test
    void certificadoConVariasFilasSaleEnUnPdf() throws Exception {
        Map<String, String> f1 = new LinkedHashMap<>();
        f1.put("nombre", "María José Benavides & Cía <prueba>");
        f1.put("documento", "1085123456");
        f1.put("motivo", "Por su participación");
        f1.put("fecha", "12/10/2026");
        Map<String, String> f2 = new LinkedHashMap<>(f1);
        f2.put("nombre", "Andrés Rosero");

        Path pdf = tmp.resolve("certificados.pdf");
        motor.generar("certificado", variables(List.of(f1, f2)), Locale.forLanguageTag("es"), pdf);
        assertEsPdf(pdf);
        // El texto se escapa: los caracteres especiales no rompen el HTML
        String html = motor.html("certificado", variables(List.of(f1)), Locale.forLanguageTag("es"));
        assertTrue(html.contains("María José Benavides &amp; Cía &lt;prueba&gt;"));
    }

    @Test
    void cuentaDeCobroYListado() throws Exception {
        Map<String, String> f = new LinkedHashMap<>();
        for (String c : List.of("cliente", "nit", "concepto", "valor", "ciudad", "fecha")) f.put(c, c.toUpperCase());
        Path cc = tmp.resolve("cc.pdf");
        motor.generar("cuenta-cobro", variables(List.of(f)), Locale.forLanguageTag("es"), cc);
        assertEsPdf(cc);

        Path listado = tmp.resolve("listado.pdf");
        Map<String, Object> v = variables(List.of());
        v.put("columnas", List.of("Producto", "Valor"));
        v.put("registros", List.of(List.of("Café", "28.000"), List.of("Panela", "6.500")));
        motor.generar("listado", v, Locale.forLanguageTag("es"), listado);
        assertEsPdf(listado);
    }

    @Test
    void lasPlantillasDeLaAppDeclaranSusCampos() throws Exception {
        ServicioPlantillas s = new ServicioPlantillas(null);
        assertEquals(List.of("nombre", "documento", "motivo", "fecha"), DetectorMarcadores.detectarHtml(s.leerHtml("certificado")).simples());
        assertEquals(List.of("cliente", "nit", "concepto", "valor", "ciudad", "fecha"), DetectorMarcadores.detectarHtml(s.leerHtml("cuenta-cobro")).simples());
        assertTrue(DetectorMarcadores.detectarHtml(s.leerHtml("listado")).simples().isEmpty());
    }

    private static Map<String, Object> variables(List<Map<String, String>> filas) {
        Map<String, Object> v = new HashMap<>();
        v.put("filas", filas);
        v.put("columnas", List.of());
        v.put("registros", List.of());
        v.put("titulo", "Prueba");
        v.put("archivo", "datos");
        v.put("fecha", "12/10/2026");
        v.put("total", filas.size());
        return v;
    }

    private static void assertEsPdf(Path p) throws Exception {
        byte[] b = Files.readAllBytes(p);
        assertTrue(b.length > 1000, "PDF vacío");
        assertEquals("%PDF", new String(b, 0, 4));
    }
}
