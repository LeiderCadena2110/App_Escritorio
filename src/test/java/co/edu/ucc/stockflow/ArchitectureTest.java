package co.edu.ucc.stockflow;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Architecture rules, checked on every build. It reads the source code and looks at what each class
 * imports or names, so a dependency that breaks the design makes the build fail.
 *
 * <p>Modules: shared, settings, catalog, customers, inventory, sales, reporting and app.
 * Layers inside a module: domain, application, infrastructure and ui.</p>
 */
class ArchitectureTest {

    private static final String BASE = "co.edu.ucc.stockflow";
    private static final Pattern PACKAGE = Pattern.compile("^package ([\\w.]+);", Pattern.MULTILINE);
    private static final Pattern PROJECT_REF = Pattern.compile("\\b" + Pattern.quote(BASE) + "((?:\\.[a-z]\\w*)+)\\.[A-Z]");

    /**
     * Which modules each module may use outside its UI layer. The graph only points "down", so there
     * are no cycles: settings ← catalog ← inventory ← sales ← reporting ← assistant, and customers on its own.
     */
    private static final Map<String, Set<String>> ALLOWED_MODULES = Map.of(
            "shared", Set.of(),
            "settings", Set.of("shared"),
            "catalog", Set.of("shared", "settings"),
            "customers", Set.of("shared"),
            "inventory", Set.of("shared", "catalog"),
            "sales", Set.of("shared", "settings", "catalog", "customers", "inventory"),
            "reporting", Set.of("shared", "settings", "catalog", "customers", "inventory", "sales"),
            "assistant", Set.of("shared", "settings", "catalog", "inventory", "reporting"));

    private record SourceClass(String file, String module, String layer, String code, List<Location> uses) {
    }

    private record Location(String module, String layer) {
    }

    private static final List<SourceClass> CLASSES = new ArrayList<>();

    @BeforeAll
    static void readSources() throws IOException {
        Path root = sourceRoot();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                String code = text.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
                Matcher pkg = PACKAGE.matcher(text);
                if (!pkg.find()) continue;
                Location me = locate(pkg.group(1));
                List<Location> uses = new ArrayList<>();
                Matcher ref = PROJECT_REF.matcher(code);
                while (ref.find()) uses.add(locate(BASE + ref.group(1)));
                CLASSES.add(new SourceClass(root.relativize(file).toString(), me.module(), me.layer(), code, uses));
            }
        }
        assertTrue(CLASSES.size() > 50, "No se encontraron las clases del proyecto en " + root);
    }

    @Test
    void domainIsPlainJava() {
        check("domain no debe usar Spring, JavaFX, SQL ni otras capas", c -> c.layer().equals("domain"), c -> {
            List<String> problems = new ArrayList<>(frameworks(c, "javafx.", "org.springframework.", "java.sql."));
            for (Location u : c.uses()) {
                boolean ok = u.layer().equals("domain") || (u.module().equals("shared") && u.layer().equals("util"));
                if (!ok) problems.add(u.module() + "." + u.layer());
            }
            return problems;
        });
    }

    @Test
    void applicationDoesNotKnowTheScreenNorTheDatabaseAdapters() {
        check("application no debe usar JavaFX, ui ni infrastructure", c -> c.layer().equals("application"), c -> {
            List<String> problems = new ArrayList<>(frameworks(c, "javafx."));
            for (Location u : c.uses()) {
                if (u.layer().equals("ui") || u.layer().equals("infrastructure")) problems.add(u.module() + "." + u.layer());
            }
            return problems;
        });
    }

    @Test
    void infrastructureDoesNotKnowTheScreen() {
        check("infrastructure no debe usar JavaFX ni ui", c -> c.layer().equals("infrastructure"), c -> {
            List<String> problems = new ArrayList<>(frameworks(c, "javafx."));
            for (Location u : c.uses()) if (u.layer().equals("ui")) problems.add(u.module() + ".ui");
            return problems;
        });
    }

    @Test
    void screensUseServicesNotRepositories() {
        check("ui no debe usar infrastructure (repositorios, PDF...)", c -> c.layer().equals("ui"),
                c -> c.uses().stream().filter(u -> u.layer().equals("infrastructure"))
                        .map(u -> u.module() + ".infrastructure").toList());
    }

    @Test
    void modulesOnlyDependDownwards() {
        check("dependencia entre módulos no permitida", c -> ALLOWED_MODULES.containsKey(c.module())
                && !c.layer().equals("ui"), c -> c.uses().stream()
                .filter(u -> !u.module().equals(c.module()))
                .filter(u -> !ALLOWED_MODULES.get(c.module()).contains(u.module()))
                .map(u -> u.module() + "." + u.layer()).toList());
    }

    @Test
    void sharedAndModulesNeverDependOnTheApplicationShell() {
        check("solo el arranque puede usar app", c -> !c.module().equals("app") && !c.module().equals("root"),
                c -> c.uses().stream().filter(u -> u.module().equals("app") || u.module().equals("root"))
                        .map(u -> u.module()).toList());
    }

    // ------------------------------------------------------------------ helpers

    private interface Rule {
        List<String> problems(SourceClass c);
    }

    private static void check(String rule, java.util.function.Predicate<SourceClass> applies, Rule test) {
        List<String> errors = new ArrayList<>();
        for (SourceClass c : CLASSES) {
            if (!applies.test(c)) continue;
            List<String> problems = test.problems(c).stream().distinct().toList();
            if (!problems.isEmpty()) errors.add(c.file() + " → " + problems);
        }
        if (!errors.isEmpty()) fail(rule + ":\n  " + String.join("\n  ", errors));
    }

    private static List<String> frameworks(SourceClass c, String... prefixes) {
        List<String> found = new ArrayList<>();
        for (String p : prefixes) if (c.code().matches("(?s).*\\b" + Pattern.quote(p) + ".*")) found.add(p + "*");
        return found;
    }

    /** "co.edu.ucc.stockflow.sales.domain.tax" → module sales, layer domain. */
    private static Location locate(String pkg) {
        String rest = pkg.equals(BASE) ? "" : pkg.substring(BASE.length() + 1);
        if (rest.isEmpty()) return new Location("root", "");
        String[] parts = rest.split("\\.");
        return new Location(parts[0], parts.length > 1 ? parts[1] : "");
    }

    /** src/main/java/co/edu/ucc/stockflow, found from the folder the tests run in (Maven: the project folder). */
    private static Path sourceRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("src/main/java/co/edu/ucc/stockflow");
            if (Files.isDirectory(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("No se encontró src/main/java desde " + Path.of("").toAbsolutePath());
    }
}
