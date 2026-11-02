package co.edu.ucc.stockflow.ui.component;

import co.edu.ucc.stockflow.util.Formats;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

/** Date pickers always show dd/MM/yyyy, whatever the language of the computer. */
public final class DatePickers {

    private DatePickers() {
    }

    public static void dayMonthYear(DatePicker... pickers) {
        for (DatePicker p : pickers) {
            p.setPromptText("dd/mm/aaaa");
            p.setConverter(new StringConverter<>() {
                @Override
                public String toString(LocalDate date) {
                    return date == null ? "" : Formats.DATE.format(date);
                }

                @Override
                public LocalDate fromString(String text) {
                    if (text == null || text.isBlank()) return null;
                    try {
                        return LocalDate.parse(text.trim(), Formats.DATE);
                    } catch (DateTimeParseException e) {
                        return p.getValue();
                    }
                }
            });
        }
    }
}
