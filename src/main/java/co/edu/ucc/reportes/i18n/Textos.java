package co.edu.ucc.reportes.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** Textos de la interfaz en español o inglés (i18n/textos*.properties). */
public final class Textos {

    private static Locale idioma = Locale.forLanguageTag("es");
    private static ResourceBundle paquete = cargar(idioma);

    private Textos() {
    }

    public static synchronized void iniciar(Locale nuevo) {
        idioma = nuevo;
        paquete = cargar(nuevo);
    }

    public static ResourceBundle paquete() {
        return paquete;
    }

    public static Locale idioma() {
        return idioma;
    }

    /** Texto por clave; si lleva {0}, {1}... se reemplazan con los argumentos. */
    public static String t(String clave, Object... args) {
        String texto;
        try {
            texto = paquete.getString(clave);
        } catch (MissingResourceException e) {
            return clave;
        }
        return args.length == 0 ? texto : new MessageFormat(texto, idioma).format(args);
    }

    private static ResourceBundle cargar(Locale l) {
        // Sin "fallback" al idioma del sistema: español es el texto base
        return ResourceBundle.getBundle("i18n/textos", l, ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }
}
