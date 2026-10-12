package co.edu.ucc.reportes.service;

import java.util.List;

/**
 * Lo que una plantilla necesita: marcadores {{campo}} y, si tiene una tabla {{filas}},
 * las columnas [campo] de esa tabla.
 */
public record Marcadores(List<String> simples, List<String> columnasTabla, boolean tieneTabla) {
}
