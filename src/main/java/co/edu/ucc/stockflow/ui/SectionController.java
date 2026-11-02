package co.edu.ucc.stockflow.ui;

/** Controller of a section: it is told every time the section is shown so it can reload its data. */
public interface SectionController {

    default void onShow() {
    }
}
