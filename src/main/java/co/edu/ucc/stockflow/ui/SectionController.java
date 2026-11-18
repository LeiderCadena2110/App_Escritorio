package co.edu.ucc.stockflow.ui;

/** Controller of a section: it is told every time the section is shown so it can reload its data. */
public interface SectionController {

    default void onShow() {
    }

    /**
     * Another section opened this one with some data (for example Products opens Inventory with a
     * product to show its history). Each section decides which requests it understands.
     */
    default void receive(Object request) {
    }
}
