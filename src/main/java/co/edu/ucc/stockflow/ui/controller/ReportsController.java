package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.dto.DailySales;
import co.edu.ucc.stockflow.dto.SalesSummary;
import co.edu.ucc.stockflow.dto.TopProduct;
import co.edu.ucc.stockflow.service.CsvExportService;
import co.edu.ucc.stockflow.service.ReportService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.ViewLoader;
import co.edu.ucc.stockflow.ui.component.DatePickers;
import co.edu.ucc.stockflow.ui.component.FileChoosers;
import co.edu.ucc.stockflow.ui.component.Tables;
import co.edu.ucc.stockflow.util.DateRange;
import co.edu.ucc.stockflow.util.Money;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ReportsController implements SectionController {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"));

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label totalLabel;
    @FXML private Label invoicesLabel;
    @FXML private Label profitLabel;
    @FXML private Label marginLabel;
    @FXML private Label ticketLabel;
    @FXML private Label unitsLabel;
    @FXML private Label taxLabel;
    @FXML private Label discountLabel;
    @FXML private BarChart<String, Number> chart;
    @FXML private NumberAxis yAxis;
    @FXML private TableView<TopProduct> topTable;

    private final ReportService reports;
    private final CsvExportService csv;
    private final Dialogs dialogs;
    private final Navigator navigator;
    private final ViewLoader views;
    private boolean updating;

    public ReportsController(ReportService reports, CsvExportService csv, Dialogs dialogs, Navigator navigator,
                             ViewLoader views) {
        this.reports = reports;
        this.csv = csv;
        this.dialogs = dialogs;
        this.navigator = navigator;
        this.views = views;
    }

    @FXML
    private void initialize() {
        topTable.getColumns().setAll(List.of(
                Tables.text("Código", TopProduct::sku, 110),
                Tables.text("Producto", TopProduct::name, 280),
                Tables.integer("Unidades", TopProduct::quantity, 90),
                Tables.money("Ventas (sin IVA)", TopProduct::revenue, 140),
                Tables.money("Ganancia", TopProduct::profit, 130)));
        Tables.fill(topTable, 1);
        Tables.placeholder(topTable, "No hay ventas en este periodo.");
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                double v = n.doubleValue();
                if (v >= 1_000_000) return String.format(Locale.forLanguageTag("es"), "%.1f M", v / 1_000_000);
                if (v >= 1_000) return String.format("%.0f mil", v / 1_000);
                return String.format("%.0f", v);
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
        DatePickers.dayMonthYear(fromDate, toDate);
        setRange(DateRange.thisMonth());
        fromDate.valueProperty().addListener((o, a, b) -> reload());
        toDate.valueProperty().addListener((o, a, b) -> reload());
    }

    @Override
    public void onShow() {
        reload();
    }

    private void setRange(DateRange r) {
        updating = true;
        fromDate.setValue(r.start().toLocalDate());
        toDate.setValue(r.end().toLocalDate().minusDays(1));
        updating = false;
        reload();
    }

    private void reload() {
        if (updating || fromDate.getValue() == null || toDate.getValue() == null) return;
        DateRange range = DateRange.ofDays(fromDate.getValue(), toDate.getValue());
        SalesSummary s = reports.salesSummary(range);
        totalLabel.setText(Money.format(s.total()));
        invoicesLabel.setText(s.invoices() + " facturas pagadas");
        profitLabel.setText(Money.format(s.profit()));
        BigDecimal base = s.subtotal().subtract(s.discount());
        marginLabel.setText(base.signum() == 0 ? "Margen —" : "Margen "
                + s.profit().multiply(BigDecimal.valueOf(100)).divide(base, 1, RoundingMode.HALF_UP)
                .toPlainString().replace('.', ',') + " % sobre ventas sin IVA");
        ticketLabel.setText(Money.format(s.averageTicket()));
        unitsLabel.setText(s.units() + " unidades vendidas");
        taxLabel.setText(Money.format(s.tax()));
        discountLabel.setText("Descuentos: " + Money.format(s.discount()));

        List<DailySales> days = reports.dailySales(range);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (days.size() <= 62) {
            for (DailySales d : days) series.getData().add(new XYChart.Data<>(DAY.format(d.date()), d.total()));
        } else {
            // Long periods: one bar per month
            java.util.Map<YearMonth, BigDecimal> byMonth = new java.util.TreeMap<>();
            for (DailySales d : days) byMonth.merge(YearMonth.from(d.date()), d.total(), BigDecimal::add);
            DateTimeFormatter month = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es"));
            byMonth.forEach((m, total) -> series.getData().add(new XYChart.Data<>(month.format(m), total)));
        }
        chart.getData().setAll(List.of(series));
        topTable.getItems().setAll(reports.topProducts(range, 50));
    }

    @FXML
    private void today() {
        setRange(DateRange.today());
    }

    @FXML
    private void lastWeek() {
        setRange(DateRange.lastDays(7));
    }

    @FXML
    private void thisMonth() {
        setRange(DateRange.thisMonth());
    }

    @FXML
    private void lastMonth() {
        YearMonth m = YearMonth.now().minusMonths(1);
        setRange(DateRange.ofDays(m.atDay(1), m.atEndOfMonth()));
    }

    @FXML
    private void exportTopProducts() {
        LocalDate from = fromDate.getValue();
        Path file = FileChoosers.saveCsv(navigator.window(), "mas-vendidos-" + (from == null ? "" : from));
        if (file == null) return;
        dialogs.run(navigator.window(), "No se pudo exportar", () -> {
            try {
                csv.exportTopProducts(topTable.getItems(), file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            views.openFile(file);
        });
    }
}
