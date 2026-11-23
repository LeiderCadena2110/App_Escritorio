package co.edu.ucc.stockflow.shared.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Small notices in the bottom right corner of the window that disappear by themselves
 * ("Factura FV-000012 registrada"). Unlike a dialog they do not stop the work: the cashier can
 * keep selling. They stay while the mouse is over them, and can offer one action (for example
 * "Guardar PDF"). Errors still use {@link Dialogs}, because they must be read.
 */
@Component
public class Toasts {

    /** Kind of notice: it only changes the color. */
    public enum Kind {
        SUCCESS("toast-success", Icon.CHECK_CIRCLE), INFO("toast-info", Icon.INFO), WARNING("toast-warning", Icon.WARNING);

        private final String styleClass;
        private final Icon icon;

        Kind(String styleClass, Icon icon) {
            this.styleClass = styleClass;
            this.icon = icon;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Toasts.class);
    private static final int MAX_VISIBLE = 3;
    private static final Duration SHORT = Duration.seconds(4);
    private static final Duration WITH_ACTION = Duration.seconds(9);

    /** Created when the window attaches it, so the bean can exist without JavaFX (tests). */
    private VBox stack;

    /** Called once by the main window with the area where the toasts float. */
    public void attach(StackPane host) {
        stack = new VBox(8);
        stack.getStyleClass().add("toast-stack");
        stack.setAlignment(Pos.BOTTOM_RIGHT);
        stack.setPickOnBounds(false); // clicks between toasts reach the screen below
        stack.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(stack, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(stack, new Insets(0, 18, 18, 0));
        host.getChildren().add(stack);
    }

    public void show(Kind kind, String title, String message) {
        show(kind, title, message, null, null);
    }

    /** Shows a notice with one button; clicking it runs the action and closes the notice. */
    public void show(Kind kind, String title, String message, String actionText, Runnable action) {
        if (stack == null) {
            log.info("{}: {}", title, message); // no window yet (for example in tests)
            return;
        }
        HBox toast = build(kind, title, message, actionText, action);
        if (stack.getChildren().size() >= MAX_VISIBLE) stack.getChildren().remove(0);
        stack.getChildren().add(toast);

        FadeTransition in = new FadeTransition(Duration.millis(180), toast);
        in.setFromValue(0);
        in.setToValue(1);
        in.play();

        PauseTransition life = new PauseTransition(action == null ? SHORT : WITH_ACTION);
        life.setOnFinished(e -> close(toast));
        toast.setOnMouseEntered(e -> life.pause());
        toast.setOnMouseExited(e -> life.play());
        life.play();
    }

    private HBox build(Kind kind, String title, String message, String actionText, Runnable action) {
        HBox toast = new HBox(12);
        toast.getStyleClass().addAll("toast", kind.styleClass);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(520);

        Region bar = new Region();
        bar.getStyleClass().add("toast-bar");
        Region icon = kind.icon.node(20);
        icon.getStyleClass().add("toast-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("toast-title");
        VBox text = new VBox(2, titleLabel);
        if (message != null && !message.isBlank()) {
            // "$ 248.472" never splits between two lines
            Label messageLabel = new Label(message.replace("$ ", "$\u00A0"));
            messageLabel.getStyleClass().add("toast-message");
            messageLabel.setWrapText(true);
            text.getChildren().add(messageLabel);
        }
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMinWidth(250);
        toast.getChildren().addAll(bar, icon, text);

        if (action != null) {
            Button button = new Button(actionText);
            button.getStyleClass().add("toast-action");
            button.setMinWidth(Region.USE_PREF_SIZE); // never cut to "..."
            button.setOnAction(e -> {
                close(toast);
                action.run();
            });
            toast.getChildren().add(button);
        }
        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("toast-close");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(e -> close(toast));
        toast.getChildren().add(closeButton);
        return toast;
    }

    private void close(Node toast) {
        if (stack == null || !stack.getChildren().contains(toast)) return;
        FadeTransition out = new FadeTransition(Duration.millis(220), toast);
        out.setToValue(0);
        out.setOnFinished(e -> stack.getChildren().remove(toast));
        out.play();
    }
}
