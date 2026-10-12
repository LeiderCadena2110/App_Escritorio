package co.edu.ucc.reportes.service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Cómo escribir fechas y números en los informes (Configuración › General y formatos). */
public record FormatoValores(String patronFecha, Locale localeNumeros, Locale idioma) {

    public static FormatoValores porDefecto() {
        return new FormatoValores("dd/MM/yyyy", Locale.forLanguageTag("es-CO"), Locale.forLanguageTag("es"));
    }

    public String fecha(LocalDateTime f) {
        String texto = DateTimeFormatter.ofPattern(patronFecha, idioma).format(f);
        if (f.getHour() != 0 || f.getMinute() != 0) {
            texto += " " + DateTimeFormatter.ofPattern("HH:mm").format(f);
        }
        return texto;
    }

    /** Número sin formato propio en Excel ("General"): enteros sin puntos (cédulas, teléfonos). */
    public String numeroGeneral(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return Long.toString((long) v);
        }
        DecimalFormat df = new DecimalFormat("0.##########", DecimalFormatSymbols.getInstance(localeNumeros));
        return df.format(v);
    }

    /** Número con separador de miles según la configuración (para totales calculados). */
    public String numeroConMiles(double v) {
        DecimalFormat df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(localeNumeros));
        return df.format(v);
    }
}
