/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pins that no embedded facade is gated on the tenant administrator alone. Embedded roles carry embedded scopes, so a
 * gate that admits only {@code isTenantAdmin()} (optionally with connected-user alternatives) locks every embedded
 * Admin out of an operation their role is meant to cover.
 */
class EmbeddedTenantAdminGateScanTest {

    private static final String PRE_AUTHORIZE_MARKER = "@PreAuthorize(";

    private static final Map<String, String> TENANT_LEVEL_OPERATIONS = Map.of(
        "EmbeddedMcpServerFacadeImpl#updateEmbeddedMcpServerSecretKey",
        "automation's rotateSecretKey is tenant-only, so the embedded rotation of the same server secret keeps the same "
            + "tenant-level rule");

    private static Map<String, String> tenantAdminOnlyGates;
    private static int embeddedGuardCount;

    @BeforeAll
    static void scanTheEmbeddedSources() {
        List<Path> embeddedSourceFiles = new ArrayList<>();

        for (Path sourceFile : MainSourceScan.collectMainSourceFiles(MainSourceScan.serverRoot())) {
            if (sourceFile.toString()
                .contains(embeddedLibsSegment())) {

                embeddedSourceFiles.add(sourceFile);
            }
        }

        tenantAdminOnlyGates = new TreeMap<>();

        for (Path sourceFile : embeddedSourceFiles) {
            String source = MainSourceScan.stripComments(MainSourceScan.readSource(sourceFile));
            String fileName = MainSourceScan.fileName(sourceFile);

            String className = fileName.substring(0, fileName.length() - ".java".length());

            embeddedGuardCount += countGuards(source);

            tenantAdminOnlyGates.putAll(findTenantAdminOnlyGates(className, source));
        }
    }

    @Test
    void testTheScanFoundTheEmbeddedGates() {
        assertThat(embeddedGuardCount)
            .as("no @PreAuthorize was found under server/ee/libs/embedded, so the scan is broken")
            .isGreaterThan(100);
    }

    @Test
    void testNoEmbeddedFacadeStillGatesOnTenantAdminAlone() {
        Map<String, String> unlistedGates = new TreeMap<>(tenantAdminOnlyGates);

        unlistedGates.keySet()
            .removeAll(TENANT_LEVEL_OPERATIONS.keySet());

        assertThat(unlistedGates)
            .as("these embedded gates admit only a tenant administrator, so an embedded Admin cannot reach them. "
                + "Gate them on an embedded scope, or list a genuinely tenant-level operation in "
                + "TENANT_LEVEL_OPERATIONS with the reason")
            .isEmpty();
    }

    @Test
    void testEveryListedTenantLevelOperationIsStillTenantAdminOnly() {
        Set<String> staleOperations = new TreeSet<>(TENANT_LEVEL_OPERATIONS.keySet());

        staleOperations.removeAll(tenantAdminOnlyGates.keySet());

        assertThat(staleOperations)
            .as("these TENANT_LEVEL_OPERATIONS entries are no longer tenant-admin-only gates, so remove them")
            .isEmpty();
        assertThat(TENANT_LEVEL_OPERATIONS.values()).allSatisfy(reason -> assertThat(reason).isNotBlank());
    }

    @Test
    void testTheScanFlagsAMethodGatedOnTenantAdminAlone() {
        String source = """
            class SampleFacadeImpl {
                @PreAuthorize("isTenantAdmin()")
                public void rotate(long id) {
                }

                @PreAuthorize("isTenantAdmin() "
                    + "or isConnectedUser()")
                public void list() {
                }

                @PreAuthorize("hasEmbeddedScope('MCP_VIEW')")
                public void view() {
                }

                @PreAuthorize("hasEmbeddedScope('MCP_VIEW') or isTenantAdmin()")
                public void viewOrAdmin() {
                }

                @PreAuthorize("#type == 1 ? hasEmbeddedScope('MCP_VIEW') : isTenantAdmin()")
                public void branch(int type) {
                }
            }
            """;

        assertThat(findTenantAdminOnlyGates("SampleFacadeImpl", source).keySet())
            .containsExactlyInAnyOrder("SampleFacadeImpl#rotate", "SampleFacadeImpl#list");
    }

    @Test
    void testTheScanFlagsAClassGatedOnTenantAdminAlone() {
        String source = """
            @Service
            @PreAuthorize("isTenantAdmin()")
            public class SampleFacadeImpl {
                public void rotate(long id) {
                }
            }
            """;

        assertThat(findTenantAdminOnlyGates("SampleFacadeImpl", source).keySet())
            .containsExactly("SampleFacadeImpl#<class>");
    }

    private static int countGuards(String source) {
        int count = 0;
        int index = source.indexOf(PRE_AUTHORIZE_MARKER);

        while (index >= 0) {
            count++;

            index = source.indexOf(PRE_AUTHORIZE_MARKER, index + PRE_AUTHORIZE_MARKER.length());
        }

        return count;
    }

    private static Map<String, String> findTenantAdminOnlyGates(String className, String source) {
        Map<String, String> gates = new TreeMap<>();

        int index = source.indexOf(PRE_AUTHORIZE_MARKER);

        while (index >= 0) {
            int argumentStart = index + PRE_AUTHORIZE_MARKER.length();
            int argumentEnd = closingParenthesis(source, argumentStart - 1);

            String expression = joinLiterals(source.substring(argumentStart, argumentEnd));

            if (isTenantAdminOnly(expression)) {
                gates.put(className + "#" + guardedMember(source, argumentEnd + 1), expression);
            }

            index = source.indexOf(PRE_AUTHORIZE_MARKER, argumentEnd);
        }

        return gates;
    }

    private static int closingParenthesis(String source, int openIndex) {
        int depth = 0;
        int index = openIndex;

        while (index < source.length()) {
            char character = source.charAt(index);

            if (character == '"') {
                index = endOfLiteral(source, index);
            } else if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;

                if (depth == 0) {
                    return index;
                }
            }

            index++;
        }

        throw new IllegalStateException("Unbalanced @PreAuthorize annotation");
    }

    private static int endOfLiteral(String source, int openQuoteIndex) {
        int index = openQuoteIndex + 1;

        while (index < source.length()) {
            char character = source.charAt(index);

            if (character == '\\') {
                index++;
            } else if (character == '"') {
                return index;
            }

            index++;
        }

        throw new IllegalStateException("Unterminated string literal");
    }

    private static String joinLiterals(String argument) {
        StringBuilder expression = new StringBuilder();

        int index = 0;

        while (index < argument.length()) {
            if (argument.charAt(index) == '"') {
                int end = endOfLiteral(argument, index);

                expression.append(argument, index + 1, end);

                index = end;
            } else if (!Character.isWhitespace(argument.charAt(index)) && argument.charAt(index) != '+') {
                expression.append("${constant}");

                while (index < argument.length() && argument.charAt(index) != '+') {
                    index++;
                }

                continue;
            }

            index++;
        }

        return expression.toString();
    }

    private static String guardedMember(String source, int fromIndex) {
        int index = fromIndex;

        while (index < source.length()) {
            char character = source.charAt(index);

            if (Character.isWhitespace(character)) {
                index++;
            } else if (character == '@') {
                index = skipAnnotation(source, index);
            } else {
                break;
            }
        }

        int headerEnd = index;

        while (headerEnd < source.length() && source.charAt(headerEnd) != '(' && source.charAt(headerEnd) != '{'
            && source.charAt(headerEnd) != ';') {

            headerEnd++;
        }

        String header = source.substring(index, headerEnd);

        for (String keyword : List.of("class ", "interface ", "enum ", "record ")) {
            if (header.startsWith(keyword) || header.contains(" " + keyword)) {
                return "<class>";
            }
        }

        String[] tokens = header.trim()
            .split("\\s+");

        return tokens[tokens.length - 1];
    }

    private static int skipAnnotation(String source, int atIndex) {
        int index = atIndex + 1;

        while (index < source.length() && (Character.isJavaIdentifierPart(source.charAt(index))
            || source.charAt(index) == '.')) {

            index++;
        }

        if (index < source.length() && source.charAt(index) == '(') {
            return closingParenthesis(source, index) + 1;
        }

        return index;
    }

    private static boolean isTenantAdminOnly(String expression) {
        if (expression.contains("${constant}") || expression.contains("&&") || expression.contains("?")
            || expression.contains(" and ")) {

            return false;
        }

        String[] alternatives = expression.split("\\s+or\\s+|\\|\\|");

        boolean tenantAdminPresent = false;

        for (String alternative : alternatives) {
            String normalized = alternative.replace(" ", "");

            while (normalized.startsWith("(") && normalized.endsWith(")")) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }

            if ("isTenantAdmin()".equals(normalized)) {
                tenantAdminPresent = true;
            } else if (!"isConnectedUser()".equals(normalized) && !normalized.startsWith("isCurrentConnectedUser(")) {
                return false;
            }
        }

        return tenantAdminPresent;
    }

    private static String embeddedLibsSegment() {
        String separator = File.separator;

        return separator + "ee" + separator + "libs" + separator + "embedded" + separator;
    }
}
