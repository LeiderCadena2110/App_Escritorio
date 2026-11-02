package co.edu.ucc.stockflow.ui.component;

import co.edu.ucc.stockflow.util.Formats;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** "Save as" dialogs that remember the last folder used. */
public final class FileChoosers {

    private static File lastFolder = new File(System.getProperty("user.home"));

    private FileChoosers() {
    }

    public static Path saveCsv(Window owner, String baseName) {
        return save(owner, "Exportar a CSV", baseName + "-" + Formats.FILE_STAMP.format(LocalDateTime.now()) + ".csv",
                new FileChooser.ExtensionFilter("CSV (Excel)", "*.csv"));
    }

    public static Path savePdf(Window owner, String fileName) {
        return save(owner, "Guardar factura en PDF", fileName + ".pdf",
                new FileChooser.ExtensionFilter("PDF", "*.pdf"));
    }

    private static Path save(Window owner, String title, String fileName, FileChooser.ExtensionFilter filter) {
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
