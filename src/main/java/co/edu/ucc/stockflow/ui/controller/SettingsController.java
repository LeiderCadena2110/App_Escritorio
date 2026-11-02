package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.dto.BusinessInfo;
import co.edu.ucc.stockflow.service.SettingsService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.ThemeManager;
import java.math.BigDecimal;
import java.nio.file.Path;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.util.Duration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SettingsController implements SectionController {

    @FXML private TextField nameField;
    @FXML private TextField taxIdField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField footerField;
    @FXML private Label businessStatus;
    @FXML private TextField taxRateField;
    @FXML private TextField prefixField;
    @FXML private TextField nextNumberField;
    @FXML private Label exampleLabel;
    @FXML private Label invoicingStatus;
    @FXML private ToggleGroup themeGroup;
    @FXML private RadioButton lightTheme;
    @FXML private RadioButton darkTheme;
    @FXML private Label dataFolderLabel;

    private final SettingsService settings;
    private final ThemeManager theme;
    private final Dialogs dialogs;
    private final Navigator navigator;

    public SettingsController(SettingsService settings, ThemeManager theme, Dialogs dialogs, Navigator navigator) {
        this.settings = settings;
        this.theme = theme;
        this.dialogs = dialogs;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        taxRateField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,3}([.,]\\d{0,2})?") ? c : null));
        nextNumberField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,9}") ? c : null));
        prefixField.textProperty().addListener((o, a, b) -> updateExample());
        nextNumberField.textProperty().addListener((o, a, b) -> updateExample());
        themeGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) return;
            String selected = b == darkTheme ? SettingsService.THEME_DARK : SettingsService.THEME_LIGHT;
            if (!selected.equals(settings.theme())) theme.change(selected);
        });
        dataFolderLabel.setText("Los datos se guardan en este equipo, en "
                + Path.of(System.getProperty("user.home"), ".stockflow"));
    }

    @Override
    public void onShow() {
        BusinessInfo b = settings.businessInfo();
        nameField.setText(b.name());
        taxIdField.setText(b.taxId());
        addressField.setText(b.address());
        phoneField.setText(b.phone());
        emailField.setText(b.email());
        footerField.setText(b.invoiceFooter());
        taxRateField.setText(settings.taxRate().stripTrailingZeros().toPlainString());
        prefixField.setText(settings.invoicePrefix());
        nextNumberField.setText(Long.toString(settings.nextInvoiceNumber()));
        (SettingsService.THEME_DARK.equals(settings.theme()) ? darkTheme : lightTheme).setSelected(true);
        businessStatus.setText("");
        invoicingStatus.setText("");
    }

    private void updateExample() {
        long next;
        try {
            next = Long.parseLong(nextNumberField.getText());
        } catch (NumberFormatException e) {
            next = 1;
        }
        exampleLabel.setText(SettingsService.formatInvoiceNumber(prefixField.getText(), next));
    }

    @FXML
    private void saveBusiness() {
        if (dialogs.run(navigator.window(), "No se pudo guardar", () -> settings.saveBusinessInfo(new BusinessInfo(
                nameField.getText(), taxIdField.getText(), addressField.getText(), phoneField.getText(),
                emailField.getText(), footerField.getText())))) {
            flash(businessStatus, "Guardado ✓");
        }
    }

    @FXML
    private void saveInvoicing() {
        if (dialogs.run(navigator.window(), "No se pudo guardar", () -> {
            String rate = taxRateField.getText().replace(',', '.');
            settings.setTaxRate(new BigDecimal(rate.isBlank() ? "0" : rate));
            settings.setInvoicePrefix(prefixField.getText());
            settings.setNextInvoiceNumber(Long.parseLong(nextNumberField.getText().isBlank() ? "1" : nextNumberField.getText()));
        })) {
            flash(invoicingStatus, "Guardado ✓");
        }
    }

    private static void flash(Label label, String text) {
        label.setText(text);
        PauseTransition p = new PauseTransition(Duration.seconds(3));
        p.setOnFinished(e -> label.setText(""));
        p.play();
    }
}
