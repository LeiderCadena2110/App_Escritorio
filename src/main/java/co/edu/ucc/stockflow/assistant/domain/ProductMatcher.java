package co.edu.ucc.stockflow.assistant.domain;

import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.shared.util.TextSearch;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds which product of the catalog an invoice line is. First by code (SKU or barcode), then by
 * name: the share of the product's words that appear in the description, ignoring accents, case
 * and short words like "de" or "x". The user always reviews the result before saving.
 */
public final class ProductMatcher {

    /** Share of the product name's words that must appear in the description. */
    static final double MIN_NAME_SCORE = 0.7;

    private ProductMatcher() {
    }

    /** The best product and how it was found (product null when NONE). */
    public record Match(Product product, MatchQuality quality) {
        static final Match NONE = new Match(null, MatchQuality.NONE);
    }

    public static Match find(SupplierInvoice.Line line, Collection<Product> products) {
        String code = line.code() == null ? "" : line.code().trim();
        if (!code.isEmpty()) {
            for (Product p : products) {
                if (code.equalsIgnoreCase(p.getSku()) || code.equals(p.getBarcode())) return new Match(p, MatchQuality.CODE);
            }
        }
        Set<String> described = words(line.description() + " " + code);
        Product best = null;
        double bestScore = 0;
        for (Product p : products) {
            double score = score(words(p.getName()), described);
            if (score > bestScore) {
                best = p;
                bestScore = score;
            }
        }
        return bestScore >= MIN_NAME_SCORE ? new Match(best, MatchQuality.NAME) : Match.NONE;
    }

    /** Share of the name's words found in the description (0 to 1). */
    static double score(Set<String> nameWords, Set<String> described) {
        if (nameWords.isEmpty()) return 0;
        long found = nameWords.stream().filter(described::contains).count();
        return found / (double) nameWords.size();
    }

    /** Normalized words of 2+ letters or any number ("Tenis running Ultra 3" → tenis, running, ultra, 3). */
    static Set<String> words(String text) {
        return Arrays.stream(TextSearch.normalize(text).split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 3 || w.chars().allMatch(Character::isDigit) && !w.isEmpty())
                .collect(Collectors.toSet());
    }
}
