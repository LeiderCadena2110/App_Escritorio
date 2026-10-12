package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.domain.EstadoEjecucion;
import co.edu.ucc.reportes.domain.ModoSalida;
import co.edu.ucc.reportes.domain.TipoPlantilla;
import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.GeneradorInforme;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/** Textos que se repiten en varias pantallas. */
final class Formatos {

    private Formatos() {
    }

    static String fechaHora(LocalDateTime f) {
        return f == null ? "" : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Textos.idioma()).format(f);
    }

    static String estado(EstadoEjecucion e) {
        return Textos.t("estado." + e.name());
    }

    static String modo(ModoSalida m) {
        return Textos.t("modo." + m.name());
    }

    static String tipo(TipoPlantilla t) {
        return Textos.t("tipo." + t.name());
    }

    static String duracion(long ms) {
        if (ms < 1000) return ms + " ms";
        long s = ms / 1000;
        return s < 60 ? Textos.t("duracion.segundos", s) : Textos.t("duracion.minutos", s / 60, s % 60);
    }

    /** "@fecha" → "(Fecha de hoy)"; columnas normales sin cambios. */
    static String columna(String c) {
        if (c == null || c.isEmpty()) return Textos.t("mapeo.sinColumna");
        if (GeneradorInforme.SEUDO_COLUMNAS.contains(c)) return Textos.t("mapeo.seudo." + c.substring(1));
        return c;
    }
}
