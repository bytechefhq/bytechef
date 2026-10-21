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

package com.bytechef.plugintools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Parses ByteChef Claude Code plugin skill documents: their {@code name}/{@code description}/{@code transports}
 * frontmatter and the {@code <!-- transport: ... --> ... <!-- /transport -->} fences inside their body. Only a file
 * named {@code SKILL.md} requires a frontmatter block; any other file (such as reference material under a skill's
 * {@code references/} directory) may omit it, in which case its name and description are empty strings and its
 * transport fences still parse from the start of the file.
 *
 * @author Ivica Cardic
 */
public final class SkillDocumentParser {

    private static final String FRONTMATTER_DELIMITER = "---";

    private static final Pattern OPENING_FENCE_PATTERN = Pattern.compile("<!--\\s*transport:\\s*(\\w+)(.*?)-->");

    private static final Pattern CLOSING_FENCE_PATTERN = Pattern.compile("<!--\\s*/transport\\s*-->");

    private static final Pattern USES_PATTERN = Pattern.compile("uses:\\s*([^\\n]*?)\\s*(?:edition:|$)");

    private static final Pattern EDITION_PATTERN = Pattern.compile("edition:\\s*(\\w+)");

    private static final String SKILL_FILE_NAME = "SKILL.md";

    private SkillDocumentParser() {
    }

    public static SkillDocument parse(Path file) throws IOException {
        String rawContent = Files.readString(file);
        List<String> lines = rawContent.lines()
            .toList();

        boolean hasFrontmatter = !lines.isEmpty() && lines.get(0)
            .strip()
            .equals(FRONTMATTER_DELIMITER);

        if (!hasFrontmatter && isSkillFile(file)) {
            throw new IllegalArgumentException("Missing frontmatter in " + file + " at line 1");
        }

        if (!hasFrontmatter) {
            List<TransportFence> fences = parseFences(lines, 0, file);

            return new SkillDocument(file, "", "", Set.of(), Set.of(), fences, rawContent);
        }

        int frontmatterClosingLineIndex = findFrontmatterClosingLineIndex(lines, file);

        String name = null;
        String description = null;
        Set<Transport> requiredTransports = new LinkedHashSet<>();
        Set<Transport> optionalTransports = new LinkedHashSet<>();

        for (int lineIndex = 1; lineIndex < frontmatterClosingLineIndex; lineIndex++) {
            String strippedLine = lines.get(lineIndex)
                .strip();
            int lineNumber = lineIndex + 1;

            if (strippedLine.startsWith("name:")) {
                name = strippedLine.substring("name:".length())
                    .strip();
            } else if (strippedLine.startsWith("description:")) {
                description = strippedLine.substring("description:".length())
                    .strip();
            } else if (strippedLine.startsWith("required:")) {
                requiredTransports =
                    parseTransportList(strippedLine.substring("required:".length()), file, lineNumber);
            } else if (strippedLine.startsWith("optional:")) {
                optionalTransports =
                    parseTransportList(strippedLine.substring("optional:".length()), file, lineNumber);
            }
        }

        List<TransportFence> fences = parseFences(lines, frontmatterClosingLineIndex + 1, file);

        return new SkillDocument(
            file, name, description, requiredTransports, optionalTransports, fences, rawContent);
    }

    private static boolean isSkillFile(Path file) {
        Path fileNamePath = file.getFileName();

        return fileNamePath != null && fileNamePath.toString()
            .equals(SKILL_FILE_NAME);
    }

    public static List<SkillDocument> parseAll(Path skillsDirectory) throws IOException {
        if (!Files.isDirectory(skillsDirectory)) {
            return List.of();
        }

        List<Path> targetFiles;

        try (Stream<Path> pathStream = Files.walk(skillsDirectory)) {
            targetFiles = pathStream
                .filter(Files::isRegularFile)
                .filter(SkillDocumentParser::isSkillOrReferenceFile)
                .sorted()
                .toList();
        }

        List<SkillDocument> skillDocuments = new ArrayList<>();

        for (Path targetFile : targetFiles) {
            skillDocuments.add(parse(targetFile));
        }

        return skillDocuments;
    }

    private static boolean isSkillOrReferenceFile(Path path) {
        Path fileNamePath = path.getFileName();

        if (fileNamePath == null) {
            return false;
        }

        String fileName = fileNamePath.toString();

        if (fileName.equals(SKILL_FILE_NAME)) {
            return true;
        }

        if (!fileName.endsWith(".md")) {
            return false;
        }

        Path parentDirectory = path.getParent();

        if (parentDirectory == null) {
            return false;
        }

        Path parentDirectoryFileNamePath = parentDirectory.getFileName();

        return parentDirectoryFileNamePath != null && "references".equals(parentDirectoryFileNamePath.toString());
    }

    private static int findFrontmatterClosingLineIndex(List<String> lines, Path file) {
        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            if (lines.get(lineIndex)
                .strip()
                .equals(FRONTMATTER_DELIMITER)) {
                return lineIndex;
            }
        }

        throw new IllegalArgumentException("Unclosed frontmatter in " + file + " starting at line 1");
    }

    private static Set<Transport> parseTransportList(String bracketedList, Path file, int lineNumber) {
        String trimmedList = bracketedList.strip();

        if (trimmedList.startsWith("[") && trimmedList.endsWith("]")) {
            trimmedList = trimmedList.substring(1, trimmedList.length() - 1);
        }

        Set<Transport> transports = new LinkedHashSet<>();

        if (trimmedList.isBlank()) {
            return transports;
        }

        for (String token : trimmedList.split(",")) {
            transports.add(parseTransportKeyword(token.strip(), file, lineNumber));
        }

        return transports;
    }

    private static List<TransportFence> parseFences(List<String> lines, int startLineIndex, Path file) {
        List<TransportFence> fences = new ArrayList<>();

        boolean insideFence = false;
        boolean insideCodeBlock = false;
        int fenceStartLineNumber = -1;
        Transport currentTransport = null;
        Edition currentEdition = null;
        List<String> currentUses = null;
        StringBuilder currentBody = null;

        for (int lineIndex = startLineIndex; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            int lineNumber = lineIndex + 1;

            Matcher openingMatcher = OPENING_FENCE_PATTERN.matcher(line);
            Matcher closingMatcher = CLOSING_FENCE_PATTERN.matcher(line);

            if (openingMatcher.find()) {
                if (insideFence) {
                    throw new IllegalArgumentException(
                        "Nested transport fence in " + file + " at line " + lineNumber);
                }

                insideFence = true;
                insideCodeBlock = false;
                fenceStartLineNumber = lineNumber;
                currentTransport = parseTransportKeyword(openingMatcher.group(1), file, lineNumber);

                String attributes = openingMatcher.group(2);

                currentUses = parseUses(attributes);
                currentEdition = parseEdition(attributes, file, lineNumber);

                if (requiresUses(currentTransport) && currentUses.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Transport fence for '" + currentTransport + "' is missing a required 'uses' attribute in "
                            + file + " at line " + lineNumber);
                }

                currentBody = new StringBuilder();

                continue;
            }

            if (closingMatcher.find()) {
                if (!insideFence) {
                    throw new IllegalArgumentException(
                        "Unmatched closing transport fence in " + file + " at line " + lineNumber);
                }

                fences.add(new TransportFence(currentTransport, currentEdition, currentUses, currentBody.toString()));
                insideFence = false;
                insideCodeBlock = false;

                continue;
            }

            if (insideFence) {
                if (line.strip()
                    .startsWith("```")) {
                    insideCodeBlock = !insideCodeBlock;
                } else if (!insideCodeBlock && line.strip()
                    .startsWith("#")) {
                    throw new IllegalArgumentException(
                        "Transport fence spans a heading in " + file + " at line " + lineNumber);
                }

                currentBody.append(line)
                    .append('\n');
            }
        }

        if (insideFence) {
            throw new IllegalArgumentException(
                "Unclosed transport fence in " + file + " starting at line " + fenceStartLineNumber);
        }

        return fences;
    }

    private static boolean requiresUses(Transport transport) {
        return transport == Transport.MCP || transport == Transport.CLI;
    }

    private static List<String> parseUses(String attributes) {
        Matcher usesMatcher = USES_PATTERN.matcher(attributes);

        if (!usesMatcher.find()) {
            return List.of();
        }

        String rawUses = usesMatcher.group(1)
            .strip();

        if (rawUses.isEmpty()) {
            return List.of();
        }

        List<String> uses = new ArrayList<>();

        for (String token : rawUses.split(",")) {
            uses.add(token.strip());
        }

        return uses;
    }

    private static Edition parseEdition(String attributes, Path file, int lineNumber) {
        Matcher editionMatcher = EDITION_PATTERN.matcher(attributes);

        if (!editionMatcher.find()) {
            return Edition.CE;
        }

        String editionValue = editionMatcher.group(1)
            .strip();

        try {
            return Edition.valueOf(editionValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(
                "Unknown edition '" + editionValue + "' in " + file + " at line " + lineNumber);
        }
    }

    private static Transport parseTransportKeyword(String transportValue, Path file, int lineNumber) {
        try {
            return Transport.valueOf(transportValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(
                "Unknown transport '" + transportValue + "' in " + file + " at line " + lineNumber);
        }
    }
}
