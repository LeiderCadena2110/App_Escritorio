package co.edu.ucc.stockflow.ui;

import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.ui.controller.InventoryController;
import co.edu.ucc.stockflow.ui.controller.PointOfSaleController;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Lets any section move to another one. */
@Component
public class Navigator {

    private MainController main;

    void register(MainController main) {
        this.main = main;
    }

    public void goTo(Section section) {
        main.show(section);
    }

    /** Opens the inventory section showing the history of one product. */
    public void showHistory(Product product) {
        main.show(Section.INVENTORY);
        main.<InventoryController>controller(Section.INVENTORY).filterByProduct(product);
    }

    /** Opens the point of sale with a product already in the cart. */
    public void sell(Product product) {
        main.show(Section.POINT_OF_SALE);
        main.<PointOfSaleController>controller(Section.POINT_OF_SALE).addProduct(product);
    }

    public Window window() {
        return main == null ? null : main.window();
    }
}
