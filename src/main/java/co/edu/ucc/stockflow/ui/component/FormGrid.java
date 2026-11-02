package co.edu.ucc.stockflow.ui.component;

import co.edu.ucc.stockflow.util.Money;
import java.math.BigDecimal;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Two-column form (label, field) used by the edit dialogs. */
public class FormGrid extends GridPane {

    private int row;

    public FormGrid() {
        setHgap(12);
        setVgap(10);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(130);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setMinWidth(260);
        getColumnConstraints().addAll(labels, fields);
    }

    public <N extends Node> N add(String label, N field) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        l.setLabelFor(field);
        add(l, 0, row);
        add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof javafx.scene.control.Control c) c.setMaxWidth(Double.MAX_VALUE);
        row++;
        return field;
    }

    /** A row that spans both columns (notes, error messages...). */
    public <N extends Node> N addWide(N node) {
        add(node, 0, row, 2, 1);
        row++;
        return node;
    }

    public static TextField text(String value, String prompt) {
        TextField f = new TextField(value == null ? "" : value);
        f.setPromptText(prompt);
        return f;
    }

    /** Field that only accepts digits (and a leading minus sign if allowed). */
    public static TextField integer(int value, boolean allowNegative) {
        TextField f = new TextField(Integer.toString(value));
        String pattern = allowNegative ? "-?\\d{0,9}" : "\\d{0,9}";
        f.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches(pattern) ? c : null));
        return f;
    }

    /** Money field: accepts digits, dots, commas and "$". */
    public static TextField money(BigDecimal value) {
        TextField f = new TextField(value == null ? "" : Money.number(value));
        f.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("[\\d.,$ ]{0,20}") ? c : null));
        f.setPromptText("0");
        return f;
    }

    public static int intValue(TextField f) {
        String t = f.getText().trim();
        if (t.isEmpty() || t.equals("-")) return 0;
        return Integer.parseInt(t);
    }

    public static BigDecimal moneyValue(TextField f) {
        return Money.parseOr(f.getText(), BigDecimal.ZERO);
    }
}
