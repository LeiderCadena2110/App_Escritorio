package co.edu.ucc.stockflow.ui.component;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

/** ComboBox helpers. */
public final class Combos {

    private Combos() {
    }

    /** Shows the given text for the empty (null) option, for example "Todas las categorías". */
    public static <T> void nullText(ComboBox<T> combo, String text) {
        combo.setCellFactory(list -> cell(text));
        combo.setButtonCell(cell(text));
    }

    private static <T> ListCell<T> cell(String nullText) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? nullText : item.toString());
            }
        };
    }
}
