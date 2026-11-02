package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.dto.DailySales;
import co.edu.ucc.stockflow.dto.DashboardSummary;
import co.edu.ucc.stockflow.dto.TopProduct;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.service.ReportService;
import co.edu.ucc.stockflow.service.SettingsService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.Section;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.component.Tables;
import co.edu.ucc.stockflow.util.Money;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class DashboardController implements SectionController {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d", Locale.forLanguageTag("es"));
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"));

    @FXML private Label greetingLabel;
    @FXML private Label dateLabel;
    @FXML private Label todaySalesLabel;
    @FXML private Label todayInvoicesLabel;
    @FXML private Label monthSalesLabel;
    @FXML private Label monthInvoicesLabel;
    @FXML private Label monthProfitLabel;
    @FXML private Label averageTicketLabel;
    @FXML private Label stockValueLabel;
    @FXML private Label productsLabel;
    @FXML private Label lowStockLabel;
    @FXML private BarChart<String, Number> salesChart;
    @FXML private NumberAxis salesAxis;
    @FXML private TableView<TopProduct> topTable;
    @FXML private TableView<Product> lowStockTable;

    private final ReportService reports;
    private final SettingsService settings;
    private final Navigator navigator;
    private final Dialogs dialogs;

    public DashboardController(ReportService reports, SettingsService settings, Navigator navigator, Dialogs dialogs) {
        this.reports = reports;
        this.settings = settings;
        this.navigator = navigator;
        this.dialogs = dialogs;
    }

    @FXML
    private void initialize() {
        topTable.getColumns().setAll(java.util.List.of(
                Tables.text("Producto", TopProduct::name, 200),
                Tables.integer("Unid.", TopProduct::quantity, 60),
                Tables.money("Ventas", TopProduct::revenue, 110)));
        Tables.fill(topTable, 0);
        Tables.placeholder(topTable, "Aún no hay ventas este mes.");

        lowStockTable.getColumns().setAll(java.util.List.of(
                Tables.text("Código", Product::getSku, 100),
                Tables.text("Producto", Product::getName, 260),
                Tables.text("Categoría", Product::getCategoryName, 140),
                Tables.integer("Stock", Product::getStock, 70),
                Tables.integer("Mínimo", Product::getMinStock, 70),
                Tables.text("Proveedor", Product::getSupplierName, 200)));
        Tables.fill(lowStockTable, 1);
        Tables.placeholder(lowStockTable, "Todo en orden: ningún producto está por debajo de su stock mínimo.");
        Tables.onOpen(lowStockTable, navigator::showHistory);

        salesAxis.setTickLabelFormatter(new StringConverter<>() {
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
    }

    @Override
    public void onShow() {
        refresh();
    }

    @FXML
    private void refresh() {
        DashboardSummary s;
        try {
            s = reports.dashboard();
        } catch (RuntimeException e) {
            dialogs.error(navigator.window(), "No se pudo cargar el inicio", e);
            return;
        }
        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? "Buenos días" : hour < 19 ? "Buenas tardes" : "Buenas noches";
        greetingLabel.setText(greeting + ", " + settings.businessInfo().name());
        String date = LONG_DATE.format(LocalDate.now());
        dateLabel.setText(Character.toUpperCase(date.charAt(0)) + date.substring(1));

        todaySalesLabel.setText(Money.format(s.today().total()));
        todayInvoicesLabel.setText(plural(s.today().invoices(), "factura", "facturas") + " · "
                + plural(s.today().units(), "unidad", "unidades"));
        monthSalesLabel.setText(Money.format(s.month().total()));
        monthInvoicesLabel.setText(plural(s.month().invoices(), "factura", "facturas"));
        monthProfitLabel.setText(Money.format(s.month().profit()));
        averageTicketLabel.setText("Ticket promedio " + Money.format(s.month().averageTicket()));
        stockValueLabel.setText(Money.format(s.stockValue()));
        productsLabel.setText(plural(s.activeProducts(), "producto", "productos") + " · "
                + plural(s.totalUnits(), "unidad", "unidades"));
        lowStockLabel.setText(Integer.toString(s.lowStockCount()));
        lowStockLabel.getStyleClass().remove("kpi-value-warning");
        if (s.lowStockCount() > 0) lowStockLabel.getStyleClass().add("kpi-value-warning");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (DailySales d : s.lastDays()) {
            series.getData().add(new XYChart.Data<>(DAY.format(d.date()), d.total()));
        }
        salesChart.getData().setAll(java.util.List.of(series));
        topTable.getItems().setAll(s.topProducts());
        lowStockTable.getItems().setAll(s.lowStock());
    }

    @FXML
    private void newSale() {
        navigator.goTo(Section.POINT_OF_SALE);
    }

    @FXML
    private void openProducts() {
        navigator.goTo(Section.PRODUCTS);
    }

    private static String plural(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }
}
