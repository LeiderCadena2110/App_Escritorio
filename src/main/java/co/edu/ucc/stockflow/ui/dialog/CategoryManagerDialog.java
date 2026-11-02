package co.edu.ucc.stockflow.ui.dialog;

import co.edu.ucc.stockflow.model.Category;
import co.edu.ucc.stockflow.service.CategoryService;
import co.edu.ucc.stockflow.ui.Dialogs;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Small window to add, rename and delete categories. */
@Component
public class CategoryManagerDialog {

    private final CategoryService categories;
    private final Dialogs dialogs;

    public CategoryManagerDialog(CategoryService categories, Dialogs dialogs) {
        this.categories = categories;
        this.dialogs = dialogs;
    }

    /** Shows the window. Returns when it is closed. */
    public void show(Window owner) {
        Dialog<Void> d = new Dialog<>();
        dialogs.prepare(d, owner, "Categorías");
        d.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);

        ListView<Category> list = new ListView<>();
        list.setPrefSize(360, 300);
        Runnable reload = () -> list.getItems().setAll(categories.findAll());
        reload.run();

        Button add = new Button("Agregar");
        add.getStyleClass().add("primary-button");
        Button rename = new Button("Renombrar");
        Button delete = new Button("Eliminar");
        delete.getStyleClass().add("danger-button");
        rename.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        Window window = owner;

        add.setOnAction(e -> dialogs.askText(window, "Nueva categoría", "Nombre:", "").ifPresent(name ->
                dialogs.run(window, "No se pudo crear la categoría", () -> {
                    categories.save(new Category(null, name, null));
                    reload.run();
                })));
        rename.setOnAction(e -> {
            Category c = list.getSelectionModel().getSelectedItem();
            dialogs.askText(window, "Renombrar categoría", "Nombre:", c.getName()).ifPresent(name ->
                    dialogs.run(window, "No se pudo renombrar", () -> {
                        categories.save(new Category(c.getId(), name, c.getDescription()));
                        reload.run();
                    }));
        });
        delete.setOnAction(e -> {
            Category c = list.getSelectionModel().getSelectedItem();
            int count = categories.countProducts(c);
            String message = count == 0 ? "¿Eliminar la categoría \"" + c.getName() + "\"?"
                    : "La categoría \"" + c.getName() + "\" tiene " + count
                    + " producto(s). Quedarán sin categoría. ¿Eliminarla?";
            if (dialogs.confirm(window, "Eliminar categoría", message, "Eliminar")) {
                dialogs.run(window, "No se pudo eliminar", () -> {
                    categories.delete(c);
                    reload.run();
                });
            }
        });

        Label hint = new Label("Las categorías agrupan los productos y sirven para filtrar.");
        hint.getStyleClass().add("muted");
        HBox buttons = new HBox(8, add, rename, delete);
        d.getDialogPane().setContent(new VBox(10, hint, list, buttons));
        d.showAndWait();
    }
}
