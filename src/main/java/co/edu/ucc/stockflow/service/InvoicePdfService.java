package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.dto.BusinessInfo;
import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.util.Formats;
import co.edu.ucc.stockflow.util.Money;
import java.awt.Color;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

/** Draws the invoice as a letter-size PDF with Apache PDFBox (no internet or external programs needed). */
@Service
public class InvoicePdfService {

    private static final PDFont REGULAR = PDType1Font.HELVETICA;
    private static final PDFont BOLD = PDType1Font.HELVETICA_BOLD;
    private static final float MARGIN = 50;
    private static final float ROW = 16;
    private static final Color ACCENT = new Color(0x1d, 0x6f, 0xb8);
    private static final Color GREY = new Color(0x61, 0x6e, 0x7c);
    private static final Color LIGHT = new Color(0xee, 0xf2, 0xf7);
    private static final Color RED = new Color(0xc0, 0x26, 0x2d);

    /** Column x positions: code, description, quantity (right), unit price (right), total (right). */
    private static final float[] COLS = {MARGIN, MARGIN + 80, MARGIN + 340, MARGIN + 430, MARGIN + 512};

    private final SettingsService settings;

    public InvoicePdfService(SettingsService settings) {
        this.settings = settings;
    }

    public void export(Invoice invoice, Path file) throws IOException {
        BusinessInfo business = settings.businessInfo();
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (PDDocument doc = new PDDocument()) {
            Page page = new Page(doc);
            float y = header(page, invoice, business);
            y = customer(page, invoice, y);
            y = tableHeader(page, y);
            boolean shaded = false;
            for (InvoiceItem item : invoice.getItems()) {
                if (y < MARGIN + 140) {
                    page.close();
                    page = new Page(doc);
                    y = tableHeader(page, page.height - MARGIN);
                }
                if (shaded) page.fill(MARGIN - 4, y - 4, page.width - 2 * MARGIN + 8, ROW, LIGHT);
                shaded = !shaded;
                page.text(REGULAR, 9, COLS[0], y, item.getProductSku(), Color.BLACK);
                page.text(REGULAR, 9, COLS[1], y, fit(item.getProductName(), REGULAR, 9, 250), Color.BLACK);
                page.right(REGULAR, 9, COLS[2], y, Integer.toString(item.getQuantity()));
                page.right(REGULAR, 9, COLS[3], y, Money.format(item.getUnitPrice()));
                page.right(REGULAR, 9, COLS[4], y, Money.format(item.getLineTotal()));
                y -= ROW;
            }
            totals(page, invoice, business, y - 10);
            page.close();
            doc.save(file.toFile());
        }
    }

    private float header(Page page, Invoice invoice, BusinessInfo b) throws IOException {
        float y = page.height - MARGIN;
        page.text(BOLD, 18, MARGIN, y, b.name(), ACCENT);
        float right = page.width - MARGIN;
        page.right(BOLD, 13, right, y, "FACTURA DE VENTA");
        y -= 16;
        page.right(BOLD, 12, right, y, invoice.getNumber());
        float left = y;
        if (!b.taxId().isBlank()) page.text(REGULAR, 9, MARGIN, left, "NIT: " + b.taxId(), GREY);
        left -= 12;
        y -= 14;
        page.right(REGULAR, 9, right, y, "Fecha: " + Formats.dateTime(invoice.getIssuedAt()));
        for (String line : new String[]{b.address(), b.phone(), b.email()}) {
            if (line != null && !line.isBlank()) {
                page.text(REGULAR, 9, MARGIN, left, line, GREY);
                left -= 12;
            }
        }
        if (invoice.isCancelled()) {
            y -= 14;
            page.rightColor(BOLD, 11, right, y, "ANULADA", RED);
        }
        y = Math.min(left, y) - 10;
        page.line(MARGIN, y, page.width - MARGIN, y, ACCENT);
        return y - 20;
    }

    private float customer(Page page, Invoice invoice, float y) throws IOException {
        page.text(BOLD, 10, MARGIN, y, "Cliente", GREY);
        y -= 14;
        page.text(BOLD, 11, MARGIN, y, Formats.blankIfNull(invoice.getCustomerName()), Color.BLACK);
        y -= 13;
        if (invoice.getCustomerDocument() != null) {
            page.text(REGULAR, 9, MARGIN, y, invoice.getCustomerDocument(), GREY);
            y -= 12;
        }
        return y - 14;
    }

    private float tableHeader(Page page, float y) throws IOException {
        page.fill(MARGIN - 4, y - 5, page.width - 2 * MARGIN + 8, ROW + 2, ACCENT);
        page.text(BOLD, 9, COLS[0], y, "Código", Color.WHITE);
        page.text(BOLD, 9, COLS[1], y, "Descripción", Color.WHITE);
        page.rightColor(BOLD, 9, COLS[2], y, "Cant.", Color.WHITE);
        page.rightColor(BOLD, 9, COLS[3], y, "Precio unit.", Color.WHITE);
        page.rightColor(BOLD, 9, COLS[4], y, "Total", Color.WHITE);
        return y - ROW - 4;
    }

    private void totals(Page page, Invoice invoice, BusinessInfo b, float y) throws IOException {
        float labelX = COLS[3] - 20;
        float valueX = COLS[4];
        page.line(labelX - 90, y + 8, valueX, y + 8, GREY);
        y -= 6;
        y = totalLine(page, labelX, valueX, y, "Subtotal", Money.format(invoice.getSubtotal()), false);
        if (invoice.getDiscount().signum() > 0) {
            y = totalLine(page, labelX, valueX, y, "Descuento", "- " + Money.format(invoice.getDiscount()), false);
        }
        String rate = invoice.getTaxRate().stripTrailingZeros().toPlainString();
        y = totalLine(page, labelX, valueX, y, "IVA (" + rate + " %)", Money.format(invoice.getTax()), false);
        y = totalLine(page, labelX, valueX, y - 2, "TOTAL", Money.format(invoice.getTotal()), true);

        y -= 10;
        page.text(REGULAR, 9, MARGIN, y, "Forma de pago: " + invoice.getPaymentMethod().label(), GREY);
        if (invoice.getPaymentMethod() == PaymentMethod.CASH) {
            y -= 12;
            page.text(REGULAR, 9, MARGIN, y, "Recibido: " + Money.format(invoice.getAmountPaid())
                    + "    Cambio: " + Money.format(invoice.getChange()), GREY);
        }
        if (invoice.getNotes() != null) {
            y -= 12;
            page.text(REGULAR, 9, MARGIN, y, fit("Notas: " + invoice.getNotes(), REGULAR, 9, page.width - 2 * MARGIN), GREY);
        }
        if (b.invoiceFooter() != null && !b.invoiceFooter().isBlank()) {
            page.center(REGULAR, 9, page.width / 2, MARGIN - 10, b.invoiceFooter());
        }
    }

    private static float totalLine(Page page, float labelX, float valueX, float y, String label, String value,
                                   boolean strong) throws IOException {
        PDFont font = strong ? BOLD : REGULAR;
        float size = strong ? 12 : 10;
        page.rightColor(font, size, labelX, y, label, strong ? ACCENT : GREY);
        page.rightColor(font, size, valueX, y, value, strong ? ACCENT : Color.BLACK);
        return y - (strong ? 18 : 14);
    }

    /** Cuts the text with "…" so it fits in the given width. */
    private static String fit(String text, PDFont font, float size, float width) throws IOException {
        String t = safe(text);
        if (width(font, size, t) <= width) return t;
        while (t.length() > 1 && width(font, size, t + "...") > width) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    private static float width(PDFont font, float size, String text) throws IOException {
        return font.getStringWidth(text) / 1000 * size;
    }

    /** The standard PDF fonts only know Windows-1252 characters; anything else becomes "?". */
    static String safe(String text) {
        if (text == null) return "";
        CharsetEncoder cp1252 = Charset.forName("windows-1252").newEncoder();
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c == '\n' || c == '\r' || c == '\t') sb.append(' ');
            else sb.append(cp1252.canEncode(c) ? c : '?');
        }
        return sb.toString();
    }

    /** One page with small drawing helpers. */
    private static final class Page {
        private final PDPageContentStream out;
        final float width;
        final float height;

        Page(PDDocument doc) throws IOException {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            width = page.getMediaBox().getWidth();
            height = page.getMediaBox().getHeight();
            out = new PDPageContentStream(doc, page);
        }

        void text(PDFont font, float size, float x, float y, String text, Color color) throws IOException {
            out.beginText();
            out.setNonStrokingColor(color);
            out.setFont(font, size);
            out.newLineAtOffset(x, y);
            out.showText(safe(text));
            out.endText();
        }

        void right(PDFont font, float size, float x, float y, String text) throws IOException {
            rightColor(font, size, x, y, text, Color.BLACK);
        }

        void rightColor(PDFont font, float size, float x, float y, String text, Color color) throws IOException {
            String t = safe(text);
            text(font, size, x - width(font, size, t), y, t, color);
        }

        void center(PDFont font, float size, float x, float y, String text) throws IOException {
            String t = safe(text);
            text(font, size, x - width(font, size, t) / 2, y, t, GREY);
        }

        void fill(float x, float y, float w, float h, Color color) throws IOException {
            out.setNonStrokingColor(color);
            out.addRect(x, y, w, h);
            out.fill();
        }

        void line(float x1, float y1, float x2, float y2, Color color) throws IOException {
            out.setStrokingColor(color);
            out.setLineWidth(0.8f);
            out.moveTo(x1, y1);
            out.lineTo(x2, y2);
            out.stroke();
        }

        void close() throws IOException {
            out.close();
        }
    }
}
