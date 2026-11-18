package co.edu.ucc.stockflow.exception;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** Turns an {@link ErrorCode} and its arguments into the text the user reads. */
public final class Messages {

    private static final Locale LOCALE = Locale.forLanguageTag("es-CO");
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("i18n/messages", LOCALE);

    private Messages() {
    }

    public static String format(ErrorCode code, Object... args) {
        String pattern;
        try {
            pattern = BUNDLE.getString(code.messageKey());
        } catch (MissingResourceException e) {
            return code.name();
        }
        return args.length == 0 ? pattern : new MessageFormat(pattern, LOCALE).format(args);
    }

    /** True when the code has a text (used by a test so no code is left without message). */
    static boolean hasMessage(ErrorCode code) {
        return BUNDLE.containsKey(code.messageKey());
    }
}
