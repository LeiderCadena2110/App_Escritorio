package co.edu.ucc.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ValidadorDatosTest {

    private static TablaDatos tabla(List<String> columnas, List<List<String>> filas) {
        List<Integer> nums = new java.util.ArrayList<>();
        for (int i = 0; i < filas.size(); i++) nums.add(i + 2);
        return new TablaDatos(null, null, "", columnas, filas, nums, List.of());
    }

    @Test
    void avisaTextoEnColumnaNumericaConFilaYColumna() {
        TablaDatos t = tabla(List.of("Cliente", "Valor"), List.of(
                List.of("A", "1.200"), List.of("B", "3500,50"), List.of("C", "abc"), List.of("D", "$ 9.000")));
        List<Problema> p = ValidadorDatos.validar(t);
        assertEquals(1, p.size());
        assertEquals(4, p.get(0).fila());
        assertEquals("Valor", p.get(0).columna());
        assertTrue(p.get(0).mensaje().contains("abc"));
        assertFalse(ValidadorDatos.hayErrores(p));
    }

    @Test
    void avisaCeldasVaciasYColumnaVacia() {
        TablaDatos t = tabla(List.of("Nombre", "Notas"), List.of(List.of("A", ""), List.of("", ""), List.of("C", "")));
        List<Problema> p = ValidadorDatos.validar(t);
        assertEquals(2, p.size());
        assertTrue(p.stream().anyMatch(x -> "Notas".equals(x.columna()) && x.fila() == null));
        assertTrue(p.stream().anyMatch(x -> "Nombre".equals(x.columna()) && x.fila() == 3));
    }

    @Test
    void reconoceNumerosYFechas() {
        assertTrue(ValidadorDatos.esNumero("1.234.567,89"));
        assertTrue(ValidadorDatos.esNumero("-15%"));
        assertFalse(ValidadorDatos.esNumero("12a"));
        assertTrue(ValidadorDatos.esFecha("05/09/2026"));
        assertTrue(ValidadorDatos.esFecha("2026-09-05 14:30"));
        assertFalse(ValidadorDatos.esFecha("ayer"));
    }
}
