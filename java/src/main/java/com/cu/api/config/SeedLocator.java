package com.cu.api.config;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds {@code data/seed.sql}, the seed file shared with the Python implementation.
 *
 * <p>The path is built with {@link Path}, never by gluing strings together with a
 * separator, so it behaves the same on Windows, macOS and Linux. It searches
 * upwards from the compiled classes and from the working directory, so it does
 * not matter whether you launched from the repository root or from {@code java/}.
 * Set the {@code SEED_SQL} environment variable to override it.
 */
public final class SeedLocator {

    private static final Path SEED_RELATIVE_PATH = Paths.get("data", "seed.sql");

    private SeedLocator() {
    }

    public static Path locate() {
        String override = System.getenv("SEED_SQL");
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        List<Path> roots = searchRoots();
        for (Path start : roots) {
            for (Path directory = start; directory != null; directory = directory.getParent()) {
                Path candidate = directory.resolve(SEED_RELATIVE_PATH);
                if (Files.isRegularFile(candidate)) {
                    return candidate.normalize();
                }
            }
        }
        throw new IllegalStateException(
                "Could not find " + SEED_RELATIVE_PATH + " above any of " + roots
                        + ". Set the SEED_SQL environment variable to point at it.");
    }

    private static List<Path> searchRoots() {
        List<Path> roots = new ArrayList<>();
        roots.add(Paths.get("").toAbsolutePath());
        Path codeSource = codeSourceDirectory();
        if (codeSource != null && !roots.contains(codeSource)) {
            roots.add(codeSource);
        }
        return roots;
    }

    private static Path codeSourceDirectory() {
        try {
            URL location = SeedLocator.class.getProtectionDomain().getCodeSource().getLocation();
            Path path = Paths.get(location.toURI());
            // Inside a packaged jar this is a zip-filesystem path, which cannot
            // be resolved against a path from the default filesystem.
            if (!path.getFileSystem().equals(FileSystems.getDefault())) {
                return null;
            }
            path = path.toAbsolutePath();
            // target/classes is a directory; a jar on the classpath is a file.
            return Files.isDirectory(path) ? path : path.getParent();
        } catch (Exception unavailable) {
            return null;
        }
    }
}
