package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.Gravedad;
import co.edu.ucc.reportes.service.Problema;
import co.edu.ucc.reportes.service.TablaDatos;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Ayudas para mostrar los datos en tabla y la lista de problemas. */
final class TablaVistaPrevia {

    /** Filas que se muestran en la vista previa (el archivo completo se usa al generar). */
    static final int MAX_FILAS = 500;

    private TablaVistaPrevia() {
    }

    /** Llena la tabla: primera columna con el número de fila del archivo y luego los datos. */
    static void llenar(TableView<List<String>> vista, TablaDatos datos) {
        vista.getColumns().clear();
        vista.getItems().clear();
        if (datos == null) return;

        TableColumn<List<String>, String> num = new TableColumn<>(Textos.t("datos.fila"));
        num.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().get(0)));
        num.setPrefWidth(56);
        num.setSortable(false);
        num.getStyleClass().add("columna-numero");
        vista.getColumns().add(num);

        for (int i = 0; i < datos.columnas().size(); i++) {
            final int indice = i + 1;
            TableColumn<List<String>, String> col = new TableColumn<>(datos.columnas().get(i));
            col.setCellValueFactory(c -> new ReadOnlyStringWrapper(indice < c.getValue().size() ? c.getValue().get(indice) : ""));
            // Ancho según el título y los primeros valores, entre 80 y 280 px
            int largo = datos.columnas().get(i).length();
            for (int f = 0; f < Math.min(50, datos.totalFilas()); f++) largo = Math.max(largo, datos.filas().get(f).get(i).length());
            col.setPrefWidth(Math.max(80, Math.min(280, largo * 8 + 24)));
            vista.getColumns().add(col);
        }
        List<List<String>> filas = new ArrayList<>();
        for (int f = 0; f < Math.min(MAX_FILAS, datos.totalFilas()); f++) {
            List<String> fila = new ArrayList<>();
            fila.add(String.valueOf(datos.numerosFila().get(f)));
            fila.addAll(datos.filas().get(f));
            filas.add(fila);
        }
        vista.getItems().setAll(filas);
    }

    static void estilizarProblemas(ListView<Problema> lista) {
        lista.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Problema p, boolean vacio) {
                super.updateItem(p, vacio);
                getStyleClass().removeAll("problema-error", "problema-aviso");
                if (vacio || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                boolean error = p.gravedad() == Gravedad.ERROR;
                getStyleClass().add(error ? "problema-error" : "problema-aviso");
                Label marca = new Label(error ? "✖" : "⚠");
                marca.getStyleClass().add("problema-marca");
                setGraphic(marca);
                setText(p.mensaje());
                setWrapText(true);
                setPrefWidth(0);
            }
        });
    }

    static String resumen(TablaDatos t, List<Problema> problemas) {
        long errores = problemas.stream().filter(p -> p.gravedad() == Gravedad.ERROR).count();
        long avisos = problemas.size() - errores;
        return Textos.t("datos.resumen", t.totalFilas(), t.columnas().size(), errores, avisos);
    }
}
