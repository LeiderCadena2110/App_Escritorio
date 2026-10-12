package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.domain.Ajuste;
import co.edu.ucc.reportes.repository.AjusteRepositorio;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Ajustes del usuario guardados en la base local (Configuración y estado de la ventana). */
@Service
public class ServicioAjustes {

    public static final String CARPETA_SALIDA = "carpeta_salida";
    public static final String FORMATO_FECHA = "formato_fecha";
    public static final String FORMATO_NUMERO = "formato_numero";
    public static final String IDIOMA = "idioma";
    public static final String TEMA = "tema";
    public static final String ULTIMA_SECCION = "ultima_seccion";
    public static final String VENTANA = "ventana";

    public static final String TEMA_CLARO = "claro";
    public static final String TEMA_OSCURO = "oscuro";

    private final AjusteRepositorio repo;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile boolean cargado;

    public ServicioAjustes(AjusteRepositorio repo) {
        this.repo = repo;
    }

    public String obtener(String clave, String porDefecto) {
        cargar();
        String v = cache.get(clave);
        return v == null || v.isBlank() ? porDefecto : v;
    }

    public void guardar(String clave, String valor) {
        cargar();
        if (valor == null) {
            cache.remove(clave);
            repo.deleteById(clave);
            return;
        }
        cache.put(clave, valor);
        repo.save(new Ajuste(clave, valor));
    }

    public Path carpetaSalida() {
        return Path.of(obtener(CARPETA_SALIDA, carpetaSalidaPorDefecto().toString()));
    }

    /** Documentos/Reportes generados (o la carpeta del usuario si no hay "Documents"). */
    public static Path carpetaSalidaPorDefecto() {
        Path home = Path.of(System.getProperty("user.home"));
        for (String nombre : new String[]{"Documents", "Documentos"}) {
            if (Files.isDirectory(home.resolve(nombre))) return home.resolve(nombre).resolve("Reportes generados");
        }
        return home.resolve("Reportes generados");
    }

    public String patronFecha() {
        return obtener(FORMATO_FECHA, "dd/MM/yyyy");
    }

    public Locale localeNumeros() {
        return Locale.forLanguageTag(obtener(FORMATO_NUMERO, "es-CO"));
    }

    public Locale idioma() {
        return Locale.forLanguageTag(obtener(IDIOMA, "es"));
    }

    public String tema() {
        return obtener(TEMA, TEMA_CLARO);
    }

    public FormatoValores formatoValores() {
        return new FormatoValores(patronFecha(), localeNumeros(), idioma());
    }

    private void cargar() {
        if (cargado) return;
        synchronized (this) {
            if (cargado) return;
            for (Ajuste a : repo.findAll()) {
                if (a.getValor() != null) cache.put(a.getClave(), a.getValor());
            }
            cargado = true;
        }
    }
}
