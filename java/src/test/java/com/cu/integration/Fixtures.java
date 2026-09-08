package com.cu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Finds and reads the shared fixtures in the repository's fixtures/ directory. */
final class Fixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Fixtures() {
    }

    static JsonNode load(String name) throws IOException {
        return MAPPER.readTree(Files.readString(directory().resolve(name)));
    }

    private static Path directory() {
        for (Path candidate = Paths.get("").toAbsolutePath();
                candidate != null;
                candidate = candidate.getParent()) {
            Path fixtures = candidate.resolve("fixtures");
            if (Files.isDirectory(fixtures)) {
                return fixtures;
            }
        }
        throw new IllegalStateException(
                "Could not find the fixtures directory above " + Paths.get("").toAbsolutePath());
    }
}
