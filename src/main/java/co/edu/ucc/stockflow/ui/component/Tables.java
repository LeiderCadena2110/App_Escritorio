package co.edu.ucc.stockflow.ui.component;

import co.edu.ucc.stockflow.util.Formats;
import co.edu.ucc.stockflow.util.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

/** Builds typed table columns in a few lines (text, numbers, money and dates). */
public final class Tables {

    private Tables() {
    }

    public static <T> TableColumn<T, String> text(String title, Function<T, String> value, double width) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(Formats.blankIfNull(value.apply(d.getValue()))));
        c.setPrefWidth(width);
        return c;
    }

    public static <T> TableColumn<T, Integer> integer(String title, Function<T, Integer> value, double width) {
        TableColumn<T, Integer> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(value.apply(d.getValue())));
        c.getStyleClass().add("numeric-column");
        c.setPrefWidth(width);
        return c;
    }

    public static <T> TableColumn<T, BigDecimal> money(String title, Function<T, BigDecimal> value, double width) {
        TableColumn<T, BigDecimal> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(value.apply(d.getValue())));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : Money.format(item));
            }
        });
        c.getStyleClass().add("numeric-column");
        c.setPrefWidth(width);
        return c;
    }

    public static <T> TableColumn<T, LocalDateTime> dateTime(String title, Function<T, LocalDateTime> value,
                                                             double width) {
        TableColumn<T, LocalDateTime> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(value.apply(d.getValue())));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : Formats.dateTime(item));
            }
        });
        c.setPrefWidth(width);
        return c;
    }

    /** Column that shows a colored badge (for example "Stock bajo" or "Anulada"). */
    public static <T> TableColumn<T, T> badge(String title, Function<T, String> text, Function<T, String> styleClass,
                                              double width) {
        TableColumn<T, T> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || text.apply(item) == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(text.apply(item));
                badge.getStyleClass().addAll("badge", styleClass.apply(item));
                setGraphic(badge);
            }
        });
        c.getStyleClass().add("center-column");
        c.setPrefWidth(width);
        c.setSortable(false);
        return c;
    }

    /** Runs the action on double click or Enter over a row. */
    public static <T> void onOpen(TableView<T> table, Consumer<T> action) {
        table.setRowFactory(t -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    action.accept(row.getItem());
                }
            });
            return row;
        });
        table.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && table.getSelectionModel().getSelectedItem() != null) {
                action.accept(table.getSelectionModel().getSelectedItem());
            }
        });
    }

    /** Adds style classes to the rows (for example "row-low-stock") and keeps double click / Enter. */
    public static <T> void styledRows(TableView<T> table, Function<T, String> styleClass, Consumer<T> onOpen) {
        String[] all = {"row-low-stock", "row-inactive", "row-cancelled", "row-entry", "row-exit"};
        table.setRowFactory(t -> {
            TableRow<T> row = new TableRow<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll(all);
                    if (!empty && item != null) {
                        String s = styleClass.apply(item);
                        if (s != null) getStyleClass().add(s);
                    }
                }
            };
            if (onOpen != null) {
                row.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                        onOpen.accept(row.getItem());
                    }
                });
            }
            return row;
        });
        if (onOpen != null) {
            table.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER
                        && table.getSelectionModel().getSelectedItem() != null) {
                    onOpen.accept(table.getSelectionModel().getSelectedItem());
                }
            });
        }
    }

    public static <T> void placeholder(TableView<T> table, String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        table.setPlaceholder(l);
    }
}
