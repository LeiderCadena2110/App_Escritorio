package co.edu.ucc.stockflow.shared.ui.component;

import co.edu.ucc.stockflow.shared.util.Formats;
import java.io.File;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** "Save as" dialogs that remember the last folder used (one instance for the whole app). */
@Component
public class FileChoosers {

    private final Clock clock;
    private File lastFolder = new File(System.getProperty("user.home"));

    public FileChoosers(Clock clock) {
        this.clock = clock;
    }

    public Path saveCsv(Window owner, String baseName) {
        String stamp = Formats.FILE_STAMP.format(LocalDateTime.now(clock));
        return save(owner, "Exportar a CSV", baseName + "-" + stamp + ".csv",
                new FileChooser.ExtensionFilter("CSV (Excel)", "*.csv"));
    }

    public Path savePdf(Window owner, String fileName) {
        return save(owner, "Guardar factura en PDF", fileName + ".pdf",
                new FileChooser.ExtensionFilter("PDF", "*.pdf"));
    }

    private Path save(Window owner, String title, String fileName, FileChooser.ExtensionFilter filter) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(fileName);
        chooser.getExtensionFilters().add(filter);
        if (lastFolder.isDirectory()) chooser.setInitialDirectory(lastFolder);
        File file = chooser.showSaveDialog(owner);
        if (file == null) return null;
        lastFolder = file.getParentFile();
        return file.toPath();
    }
}
