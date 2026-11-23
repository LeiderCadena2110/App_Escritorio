package co.edu.ucc.stockflow.assistant.application;

import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * A file sent to the model together with the prompt (a photo or PDF of an invoice).
 *
 * @param mimeType for example "image/jpeg" or "application/pdf"
 * @param data     the bytes of the file
 */
public record Attachment(String mimeType, byte[] data) {

    /** Files are sent inside the request, which Gemini accepts up to about 20 MB in total. */
    public static final int MAX_MEGABYTES = 15;

    private static final Map<String, String> TYPES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp",
            "pdf", "application/pdf");

    /** Reads a photo or PDF, checking its type and size. */
    public static Attachment read(Path file) {
        String name = file.getFileName().toString();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        String type = TYPES.get(extension);
        if (type == null) throw new BusinessException(ErrorCode.AI_FILE_TYPE_NOT_SUPPORTED);
        try {
            long size = Files.size(file);
            if (size > MAX_MEGABYTES * 1024L * 1024L) {
                throw new BusinessException(ErrorCode.AI_FILE_TOO_LARGE, Math.ceil(size / (1024.0 * 1024.0)), MAX_MEGABYTES);
            }
            return new Attachment(type, Files.readAllBytes(file));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.AI_FILE_UNREADABLE, name);
        }
    }
}
