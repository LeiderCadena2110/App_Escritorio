package co.edu.ucc.stockflow.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.concurrent.Task;
import org.springframework.stereotype.Component;

/**
 * Runs database queries and file exports outside the JavaFX thread so the window never freezes.
 *
 * <p>The work runs in one background thread (in order, one after the other, which is what the
 * local H2 database likes). The result comes back to the JavaFX thread, where it is safe to
 * update the controls. Errors are shown in a dialog.</p>
 */
@Component
public class TaskRunner {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "stockflow-worker");
        t.setDaemon(true);
        return t;
    });
    private final ReadOnlyIntegerWrapper running = new ReadOnlyIntegerWrapper(0);
    private final Dialogs dialogs;
    private final Navigator navigator;

    public TaskRunner(Dialogs dialogs, Navigator navigator) {
        this.dialogs = dialogs;
        this.navigator = navigator;
    }

    /**
     * Runs {@code work} in the background and gives its result to {@code onSuccess} on the JavaFX thread.
     * Call it from the JavaFX thread.
     *
     * @param errorTitle title of the dialog shown if the work fails
     */
    public <T> void run(String errorTitle, Callable<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        running.set(running.get() + 1);
        task.setOnSucceeded(e -> {
            running.set(running.get() - 1);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            running.set(running.get() - 1);
            dialogs.error(navigator.window(), errorTitle, task.getException());
        });
        worker.execute(task);
    }

    /** Same as {@link #run(String, Callable, Consumer)} for work that returns nothing. */
    public void run(String errorTitle, ThrowingRunnable work, Runnable onSuccess) {
        run(errorTitle, () -> {
            work.run();
            return null;
        }, ignored -> onSuccess.run());
    }

    /** Number of tasks still working (the main window shows "Cargando…" while it is above 0). */
    public ReadOnlyIntegerProperty runningProperty() {
        return running.getReadOnlyProperty();
    }

    /** A piece of work that may throw checked exceptions (for example IOException when writing a file). */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
