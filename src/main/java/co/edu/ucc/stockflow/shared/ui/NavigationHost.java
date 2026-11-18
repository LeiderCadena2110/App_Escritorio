package co.edu.ucc.stockflow.shared.ui;

import javafx.stage.Window;

/** What the main window offers to {@link Navigator}: showing a section and giving its window. */
public interface NavigationHost {

    /** Shows the section and returns its controller (so a request can be passed to it). */
    Object show(Section section);

    Window window();
}
