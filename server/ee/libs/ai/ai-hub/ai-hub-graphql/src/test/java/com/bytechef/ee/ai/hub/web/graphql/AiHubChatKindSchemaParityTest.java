/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Pins that every {@link AiHubChatKind} Java enum constant is also declared in the GraphQL schema's
 * {@code AiHubChatKind} enum type ({@code graphql/ai-hub-chat.graphqls}), so that reading a chat of any kind does not
 * fail enum coercion.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatKindSchemaParityTest {

    private static final String SCHEMA_RESOURCE = "graphql/ai-hub-chat.graphqls";
    private static final String ENUM_NAME = "AiHubChatKind";

    @Test
    void testEveryJavaEnumConstantIsDeclaredInTheGraphQlSchema() throws IOException {
        List<String> schemaEnumValues = extractEnumValues(readSchema(), ENUM_NAME);

        for (AiHubChatKind kind : AiHubChatKind.values()) {
            assertThat(schemaEnumValues)
                .as("AiHubChatKind.%s must be declared in %s's %s enum", kind.name(), SCHEMA_RESOURCE, ENUM_NAME)
                .contains(kind.name());
        }
    }

    private static String readSchema() throws IOException {
        try (InputStream inputStream = AiHubChatKindSchemaParityTest.class.getClassLoader()
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
