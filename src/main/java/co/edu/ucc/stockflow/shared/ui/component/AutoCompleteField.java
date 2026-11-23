package co.edu.ucc.stockflow.shared.ui.component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

/**
 * One text box to find and choose an item: type part of the name or document, the matches appear
 * below, and Enter or a click picks one. Replaces the "search box + combo box" pair.
 *
 * <p>The chosen item is {@link #valueProperty()}. While the user types, the value does not change;
 * if they leave the box without choosing, the text goes back to the current value.</p>
 *
 * @param <T> the type of the items (for example a customer)
 */
public class AutoCompleteField<T> extends TextField {

    private static final int MAX_SUGGESTIONS = 8;

    private final ObjectProperty<T> value = new SimpleObjectProperty<>();
    private final ContextMenu popup = new ContextMenu();
    private final ObservableList<T> items;
    private final Function<T, String> title;
    private final Function<T, String> detail;
    private final BiPredicate<T, String> matcher;
    private List<T> suggestions = List.of();
    private boolean writing;

    /**
     * @param items   the items to choose from (the list can change later)
     * @param title   main text of an item, shown in the box once it is chosen
     * @param detail  secondary text shown under the title in the suggestions (may return null)
     * @param matcher whether an item matches what the user typed
     */
    public AutoCompleteField(ObservableList<T> items, Function<T, String> title, Function<T, String> detail,
                             BiPredicate<T, String> matcher) {
        this.items = items;
        this.title = title;
        this.detail = detail;
        this.matcher = matcher;
        getStyleClass().add("autocomplete-field");
        popup.getStyleClass().add("autocomplete-popup");

        value.addListener((o, old, now) -> showValue());
        textProperty().addListener((o, old, text) -> {
            if (!writing && isFocused()) suggest(text);
        });
        focusedProperty().addListener((o, was, focused) -> {
            if (focused) {
                selectAll();
            } else {
                popup.hide();
                showValue();
            }
        });
        setOnAction(e -> {
            if (!suggestions.isEmpty()) choose(suggestions.get(0));
        });
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                showValue();
                selectAll();
            } else if (e.getCode() == KeyCode.DOWN && popup.isShowing()) {
                // Lets the arrow keys move through the suggestions
                var first = popup.getSkin() == null ? null : popup.getSkin().getNode().lookup(".menu-item");
                if (first != null) first.requestFocus();
            }
        });
    }

    public ObjectProperty<T> valueProperty() {
        return value;
    }

    public T getValue() {
        return value.get();
    }

    public void setValue(T item) {
        value.set(item);
        showValue(); // also when the same item is set again
    }

    /** Selects the first item that is equal to {@code item} (useful after reloading the list). */
    public void select(Collection<? extends T> candidates, T item) {
        candidates.stream().filter(c -> Objects.equals(c, item)).findFirst().ifPresent(this::setValue);
    }

    private void choose(T item) {
        popup.hide();
        setValue(item);
        selectAll();
    }

    private void showValue() {
        writing = true;
        setText(value.get() == null ? "" : title.apply(value.get()));
        writing = false;
    }

    private void suggest(String text) {
        String query = text == null ? "" : text.trim();
        if (query.isEmpty()) {
            suggestions = List.of();
            popup.hide();
            return;
        }
        suggestions = items.stream().filter(i -> matcher.test(i, query)).limit(MAX_SUGGESTIONS).toList();
        if (suggestions.isEmpty()) {
            popup.getItems().setAll(emptyItem());
        } else {
            popup.getItems().setAll(suggestions.stream().map(this::menuItem).toList());
        }
        if (!popup.isShowing() && getScene() != null) popup.show(this, Side.BOTTOM, 0, 2);
    }

    private CustomMenuItem menuItem(T item) {
        Label main = new Label(title.apply(item));
        main.getStyleClass().add("autocomplete-title");
        VBox box = new VBox(1, main);
        String extra = detail.apply(item);
        if (extra != null && !extra.isBlank()) {
            Label sub = new Label(extra);
            sub.getStyleClass().add("autocomplete-detail");
            box.getChildren().add(sub);
        }
        box.setPrefWidth(Math.max(220, getWidth() - 24));
        CustomMenuItem menuItem = new CustomMenuItem(box, true);
        menuItem.setOnAction(e -> choose(item));
        return menuItem;
    }

    private CustomMenuItem emptyItem() {
        Label none = new Label("Sin resultados");
        none.getStyleClass().add("autocomplete-detail");
        CustomMenuItem menuItem = new CustomMenuItem(none, false);
        menuItem.setDisable(true);
        return menuItem;
    }
}
