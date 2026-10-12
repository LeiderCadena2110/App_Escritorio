package co.edu.ucc.reportes.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * PDF desde las plantillas HTML de la app: Thymeleaf llena el HTML y OpenHTMLtoPDF lo
 * convierte a PDF con las reglas @page de CSS. Java puro: sin navegador ni internet.
 */
@Component
public class MotorPdf {

    private final TemplateEngine motor;
    private final String baseUri;

    public MotorPdf() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver(MotorPdf.class.getClassLoader());
        resolver.setPrefix("plantillas-html/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        motor = new TemplateEngine();
        motor.setTemplateResolver(resolver);
        URL base = MotorPdf.class.getResource("/plantillas-html/");
        baseUri = base == null ? null : base.toExternalForm();
    }

    /** HTML ya lleno (sirve también para pruebas). */
    public String html(String recurso, Map<String, Object> variables, Locale idioma) {
        return motor.process(recurso, new Context(idioma, variables));
    }

    public void generar(String recurso, Map<String, Object> variables, Locale idioma, Path destino) throws IOException {
        String html = html(recurso, variables, idioma);
        try (OutputStream os = Files.newOutputStream(destino)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, baseUri);
            builder.toStream(os);
            builder.run();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
