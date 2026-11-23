package co.edu.ucc.stockflow.assistant.ui;

import co.edu.ucc.stockflow.assistant.application.AiModel;
import co.edu.ucc.stockflow.assistant.application.AiSettings;
import co.edu.ucc.stockflow.shared.ui.Dialogs;
import co.edu.ucc.stockflow.shared.ui.TaskRunner;
import co.edu.ucc.stockflow.shared.ui.ViewLoader;
import co.edu.ucc.stockflow.shared.ui.dialog.FormDialog;
import co.edu.ucc.stockflow.shared.ui.component.FormGrid;
import java.util.Optional;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Key and model of Gemini. Saving also tries a tiny request, so a wrong key is noticed at once. */
@Component
public class AiConfigDialog {

    static final String KEYS_PAGE = "https://aistudio.google.com/apikey";

    private final AiSettings settings;
    private final AiModel ai;
    private final Dialogs dialogs;
    private final TaskRunner tasks;
    private final ViewLoader views;

    public AiConfigDialog(AiSettings settings, AiModel ai, Dialogs dialogs, TaskRunner tasks, ViewLoader views) {
        this.settings = settings;
        this.ai = ai;
        this.dialogs = dialogs;
        this.tasks = tasks;
        this.views = views;
    }

    /** Returns the model's description when the connection test worked. */
    public Optional<String> show(Window owner) {
        FormDialog<String> fd = new FormDialog<>(dialogs, tasks, owner, "Configurar inteligencia artificial",
                "Guardar y probar");
        FormGrid form = fd.form();

        Label intro = new Label("StockFlow usa Gemini de Google. La clave es gratuita: créela con su cuenta "
                + "de Google y péguela aquí.");
        intro.setWrapText(true);
        intro.setMaxWidth(470);
        form.addWide(intro);
        Hyperlink link = new Hyperlink("Crear una clave en Google AI Studio");
        link.setOnAction(e -> views.openUrl(KEYS_PAGE));
        form.addWide(link);

        PasswordField key = form.add("Clave de Gemini", new PasswordField());
        key.setPromptText("AIza…");
        if (settings.keyFromEnvironment()) {
            key.setDisable(true);
            key.setPromptText("Se usa la variable " + AiSettings.ENVIRONMENT_VARIABLE);
        } else {
            key.setText(settings.apiKey());
        }
        TextField model = form.add("Modelo", FormGrid.text(settings.model(), AiSettings.DEFAULT_MODEL));

        Label privacy = new Label("Se envían a Google las fotos de facturas que usted elija y los nombres y "
                + "cantidades de productos del plan de compras; nunca datos de sus clientes. En el plan gratuito "
                + "Google puede usar esa información para mejorar sus servicios.");
        privacy.getStyleClass().add("muted");
        privacy.setWrapText(true);
        privacy.setMaxWidth(470);
        form.addWide(privacy);

        return fd.show(() -> {
            String newKey = settings.keyFromEnvironment() ? null : key.getText();
            String newModel = model.getText();
            return () -> {
                if (newKey != null) settings.saveKey(newKey);
                settings.saveModel(newModel);
                ai.generateText("Responde solamente con la palabra OK.");
                return ai.description();
            };
        }, settings.keyFromEnvironment() ? model : key);
    }
}
