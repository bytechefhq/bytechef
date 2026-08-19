/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactKind;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Pins that every {@link AiHubChatArtifactKind} Java enum constant is also declared in the GraphQL schema's
 * {@code AiHubChatArtifactKind} enum type ({@code graphql/ai-hub-artifact.graphqls}).
 *
 * <p>
 * The relationship is one-directional by design: the schema enum only needs to be a SUPERSET of the Java enum. A schema
 * value with no matching Java constant is harmless dead schema, but a Java constant missing from the schema is not —
 * {@code AiHubChatArtifactGraphQlController} binds {@code kind} straight onto the Java enum inside non-null GraphQL
 * wrappers, so graphql-java's coercion of an undeclared enum value throws and nulls out the ENTIRE
 * {@code aiHubChatArtifacts} response the first time a row of that kind is read (not just that one field/row). This
 * test exists because that drift shipped once already ({@code AI_AGENT_REFERENCED} was added to the Java enum without
 * the matching schema value) — it is meant to catch the same class of mistake on every future append.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatArtifactKindSchemaParityTest {

    private static final String SCHEMA_RESOURCE = "graphql/ai-hub-artifact.graphqls";
    private static final String ENUM_NAME = "AiHubChatArtifactKind";

    @Test
    void testEveryJavaEnumConstantIsDeclaredInTheGraphQlSchema() throws IOException {
        List<String> schemaEnumValues = extractEnumValues(readSchema(), ENUM_NAME);

        for (AiHubChatArtifactKind kind : AiHubChatArtifactKind.values()) {
            assertThat(schemaEnumValues)
                .as(
                    "AiHubChatArtifactKind.%s must be declared in %s's %s enum, or reads of that kind will null out " +
                        "the whole aiHubChatArtifacts response",
                    kind.name(), SCHEMA_RESOURCE, ENUM_NAME)
                .contains(kind.name());
        }
    }

    private static String readSchema() throws IOException {
        try (InputStream inputStream = AiHubChatArtifactKindSchemaParityTest.class.getClassLoader()
            .getResourceAsStream(SCHEMA_RESOURCE)) {

            if (inputStream == null) {
                throw new IllegalStateException(SCHEMA_RESOURCE + " not found on the test classpath");
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> extractEnumValues(String schema, String enumName) {
        Matcher matcher = Pattern.compile("enum\\s+" + enumName + "\\s*\\{([^}]*)}")
            .matcher(schema);

        if (!matcher.find()) {
            throw new IllegalStateException("enum " + enumName + " not found in " + SCHEMA_RESOURCE);
        }

        return Arrays.stream(matcher.group(1)
            .split("\\s+"))
            .filter(token -> !token.isBlank())
            .toList();
    }
}
