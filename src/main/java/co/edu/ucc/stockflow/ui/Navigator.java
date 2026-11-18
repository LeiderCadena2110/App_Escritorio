package co.edu.ucc.stockflow.ui;

import javafx.stage.Window;
import org.springframework.stereotype.Component;

/**
 * Lets any section move to another one, optionally with some data, without knowing the other
 * section's controller class (so modules do not depend on each other's screens).
 */
@Component
public class Navigator {

    private NavigationHost host;

    /** Called once by the main window. */
    public void register(NavigationHost host) {
        this.host = host;
    }

    public void goTo(Section section) {
        host.show(section);
    }

    /** Opens the section and gives it the request (see {@link SectionController#receive(Object)}). */
    public void open(Section section, Object request) {
        if (host.show(section) instanceof SectionController controller) controller.receive(request);
    }

    public Window window() {
        return host == null ? null : host.window();
    }
}
