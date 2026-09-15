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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads every {@code @PreAuthorize} expression in the tree as plain text, for the tree-wide guard coverage assertions
 * that no single module's test classpath could make: {@link ResourceTokenResolverCoverageTest} asks whether each
 * resource token has a resolver, {@link PermissionScopeGateCoverageTest} asks whether each catalogued scope has a gate.
 * Both need the same expressions, so both read them from here rather than each carrying its own scanner — two scanners
 * would drift, and a scanner that quietly stops seeing part of the tree makes every assertion built on it vacuously
 * true.
 * <p>
 * The scan runs over source text rather than over the classpath on purpose: guards, resolvers and the scope catalogue
 * are spread across CE, EE and the AI modules, and no single module's test classpath contains them all.
 *
 * @author Ivica Cardic
 */
final class MainSourceScan {

    private static final String JAVA_EXTENSION = ".java";
    private static final String PRE_AUTHORIZE_MARKER = "@PreAuthorize(";
    private static final Set<String> SKIPPED_DIRECTORY_NAMES =
        Set.of(".git", ".gradle", "bin", "build", "node_modules");

    private static final Pattern STRING_CONSTANT_DECLARATION =
        Pattern.compile("String\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*\"([^\"]*)\"\\s*;");

    private MainSourceScan() {
    }

    /**
     * One {@code @PreAuthorize} expression with every constant reference in it already substituted, together with the
     * simple name of the file it guards.
     */
    record ResolvedGuardExpression(String fileName, String expression) {
    }

    /**
     * Every readable guard expression in the tree, plus the annotation fragments that could not be turned into text at
     * all — reported rather than swallowed, because an expression this scan reads only in part names tokens and scopes
     * no assertion can see.
     */
    record GuardExpressionScan(
        List<ResolvedGuardExpression> guardExpressions, Map<String, Set<String>> unreadableArgumentsByFile) {
    }

    /**
     * The one pass over the tree that reads guard expressions. Comments are stripped first: several Javadoc blocks
     * quote guard expressions, and counting those would report guards no code actually declares.
     * <p>
     * Guards are not always plain literals — {@code "hasAuthority(\"" + AuthorityConstants.ADMIN + "\")"} is the common
     * shape in this tree — so each non-literal fragment is kept in place as a {@code ${...}} marker and then resolved
     * against the constant it names. Reading a literal only, as this scan first did, would make a guard such as
     * {@code "hasPermission(#id, '" + RESOURCE + "', 'SCOPE')"} name nothing at all, and the coverage assertions would
     * pass over it in silence. A constant is looked up in the guarded file itself or by opening the one file named
     * after its qualifier — a handful of reads, not another walk of the tree.
     */
    static GuardExpressionScan scanGuardExpressions(List<Path> sourceFiles) {
        Map<String, List<Path>> filesByClassName = indexFilesByClassName(sourceFiles);

        List<GuardExpression> guardExpressions = new ArrayList<>();
        Map<Path, Map<String, String>> constantsByDeclaringFile = new LinkedHashMap<>();

        for (Path sourceFile : sourceFiles) {
            String source = readSource(sourceFile);

            if (!source.contains(PRE_AUTHORIZE_MARKER)) {
                continue;
            }

            String strippedSource = stripComments(source);

            for (PreAuthorizeArgument argument : extractPreAuthorizeArguments(strippedSource)) {
                guardExpressions.add(new GuardExpression(sourceFile, argument));

                Set<String> fragments = argument.nonLiteralFragments();

                if (!fragments.isEmpty()) {
                    // Read out of the source already in hand, so a constant declared in the guarded class itself costs
                    // no extra file read.
                    constantsByDeclaringFile.computeIfAbsent(sourceFile,
                        file -> collectStringConstants(strippedSource));
                }
            }
        }

        List<ResolvedGuardExpression> resolvedGuardExpressions = new ArrayList<>();
        Map<String, Set<String>> unreadableArgumentsByFile = new TreeMap<>();

        for (GuardExpression guardExpression : guardExpressions) {
            PreAuthorizeArgument argument = guardExpression.argument();
            Path sourceFile = guardExpression.sourceFile();

            String expression = argument.expression();

            for (String fragment : argument.nonLiteralFragments()) {
                String value = resolveFragment(
                    fragment, constantsByDeclaringFile.getOrDefault(sourceFile, Map.of()), filesByClassName);

                if (value == null) {
                    Set<String> unreadableFragments = unreadableArgumentsByFile.computeIfAbsent(
                        fileName(sourceFile), file -> new TreeSet<>());

                    unreadableFragments.add(fragment);

                    continue;
                }

                expression = expression.replace(fragmentMarker(fragment), value);
            }

            resolvedGuardExpressions.add(new ResolvedGuardExpression(fileName(sourceFile), expression));
        }

        return new GuardExpressionScan(resolvedGuardExpressions, unreadableArgumentsByFile);
    }

    /**
     * The literal value behind one non-literal fragment, or {@code null} when this scan cannot establish it. An
     * annotation value must be a compile-time constant, so the fragment is always a constant reference: it is looked up
     * first among the constants of the file that declares the guard, then — when qualified — by opening the one file
     * named after the qualifier. A qualifier naming no file, or several files disagreeing about the value, yields
     * {@code null} so the guard is reported as unreadable rather than quietly treated as empty.
     */
    private static String resolveFragment(
        String fragment, Map<String, String> declaringFileConstants, Map<String, List<Path>> filesByClassName) {

        int constantSeparatorIndex = fragment.lastIndexOf('.');

        if (constantSeparatorIndex < 0) {
            return declaringFileConstants.get(fragment);
        }

        String constantName = fragment.substring(constantSeparatorIndex + 1);
        String qualifier = fragment.substring(0, constantSeparatorIndex);
        String className = qualifier.substring(qualifier.lastIndexOf('.') + 1);

        Set<String> candidateValues = new TreeSet<>();

        for (Path sourceFile : filesByClassName.getOrDefault(className, List.of())) {
            Map<String, String> constants = collectStringConstants(stripComments(readSource(sourceFile)));

            String value = constants.get(constantName);

            if (value != null) {
                candidateValues.add(value);
            }
        }

        if (candidateValues.size() == 1) {
            return candidateValues.iterator()
                .next();
        }

        // A nested class or an interface inside the guarded file: the qualifier names no file of its own, but the
        // constant is declared right there.
        return declaringFileConstants.get(constantName);
    }

    private static Map<String, List<Path>> indexFilesByClassName(List<Path> sourceFiles) {
        Map<String, List<Path>> filesByClassName = new LinkedHashMap<>();

        for (Path sourceFile : sourceFiles) {
            String name = fileName(sourceFile);

            List<Path> files = filesByClassName.computeIfAbsent(
                name.substring(0, name.length() - JAVA_EXTENSION.length()), className -> new ArrayList<>());

            files.add(sourceFile);
        }

        return filesByClassName;
    }

    /**
     * Every {@code String NAME = "literal"} declaration in the given stripped source, keyed by name. A name declared
     * twice with different values is dropped rather than guessed at.
     */
    private static Map<String, String> collectStringConstants(String strippedSource) {
        Map<String, String> constants = new LinkedHashMap<>();
        Set<String> ambiguousNames = new TreeSet<>();

        Matcher declarationMatcher = STRING_CONSTANT_DECLARATION.matcher(strippedSource);

        while (declarationMatcher.find()) {
            String name = declarationMatcher.group(1);
            String value = declarationMatcher.group(2);

            String previousValue = constants.putIfAbsent(name, value);

            if (previousValue != null && !previousValue.equals(value)) {
                ambiguousNames.add(name);
            }
        }

        for (String ambiguousName : ambiguousNames) {
            constants.remove(ambiguousName);
        }

        return constants;
    }

    private static String fragmentMarker(String fragment) {
        return "${" + fragment + "}";
    }

    /**
     * One {@code @PreAuthorize} argument: the concatenated content of its string literals with a {@code ${...}} marker
     * standing in for every non-literal fragment, and the fragment names themselves. A fragment that cannot be resolved
     * to a literal means the expression cannot be trusted to have been read in full.
     */
    private record PreAuthorizeArgument(String expression, Set<String> nonLiteralFragments) {
    }

    private record GuardExpression(Path sourceFile, PreAuthorizeArgument argument) {
    }

    /**
     * The argument of every {@code @PreAuthorize(...)} in the source, handling both multi-line annotations and
     * expressions built with {@code +}. Parenthesis depth is tracked outside string literals so an expression
     * containing {@code (} does not truncate the scan, and every character seen outside a literal is kept as the
     * non-literal residue so a guard built from a constant cannot pass as an empty expression.
     */
    private static List<PreAuthorizeArgument> extractPreAuthorizeArguments(String source) {
        List<PreAuthorizeArgument> arguments = new ArrayList<>();

        int searchFrom = 0;

        while (true) {
            int markerIndex = source.indexOf(PRE_AUTHORIZE_MARKER, searchFrom);

            if (markerIndex < 0) {
                break;
            }

            StringBuilder expression = new StringBuilder();
            StringBuilder fragment = new StringBuilder();
            Set<String> fragments = new TreeSet<>();

            int index = markerIndex + PRE_AUTHORIZE_MARKER.length();
            int depth = 1;
            boolean inString = false;
            boolean escaped = false;

            while (index < source.length() && depth > 0) {
                char character = source.charAt(index);

                if (inString) {
                    if (escaped) {
                        expression.append(character);

                        escaped = false;
                    } else if (character == '\\') {
                        escaped = true;
                    } else if (character == '"') {
                        inString = false;
                    } else {
                        expression.append(character);
                    }
                } else {
                    if (character == '"') {
                        inString = true;
                    } else if (character == '(') {
                        depth++;
                    } else if (character == ')') {
                        depth--;
                    } else if (!Character.isWhitespace(character) && character != '+') {
                        fragment.append(character);

                        index++;

                        continue;
                    }

                    flushFragment(expression, fragment, fragments);
                }

                index++;
            }

            flushFragment(expression, fragment, fragments);

            arguments.add(new PreAuthorizeArgument(expression.toString(), fragments));

            searchFrom = index;
        }

        return arguments;
    }

    /**
     * Closes off the non-literal fragment being read, leaving a {@code ${...}} marker at the position it occupied so
     * the constant behind it can be substituted back into the expression later.
     */
    private static void flushFragment(StringBuilder expression, StringBuilder fragment, Set<String> fragments) {
        if (fragment.isEmpty()) {
            return;
        }

        String fragmentName = fragment.toString();

        fragment.setLength(0);

        fragments.add(fragmentName);

        expression.append(fragmentMarker(fragmentName));
    }

    /**
     * Java source with line and block comments removed, string and character literals left intact.
     */
    static String stripComments(String source) {
        StringBuilder stripped = new StringBuilder(source.length());

        int index = 0;
        int length = source.length();

        while (index < length) {
            char character = source.charAt(index);

            if (character == '"' || character == '\'') {
                index = appendLiteral(stripped, source, index, character);

                continue;
            }

            if (character == '/' && index + 1 < length && source.charAt(index + 1) == '/') {
                while (index < length && source.charAt(index) != '\n') {
                    index++;
                }

                continue;
            }

            if (character == '/' && index + 1 < length && source.charAt(index + 1) == '*') {
                index += 2;

                while (index + 1 < length && !(source.charAt(index) == '*' && source.charAt(index + 1) == '/')) {
                    index++;
                }

                index += 2;

                continue;
            }

            stripped.append(character);

            index++;
        }

        return stripped.toString();
    }

    private static int appendLiteral(StringBuilder stripped, String source, int startIndex, char quote) {
        stripped.append(quote);

        int index = startIndex + 1;
        int length = source.length();

        while (index < length) {
            char character = source.charAt(index);

            stripped.append(character);

            index++;

            if (character == '\\') {
                if (index < length) {
                    stripped.append(source.charAt(index));

                    index++;
                }

                continue;
            }

            if (character == quote) {
                break;
            }
        }

        return index;
    }

    static String fileName(Path sourceFile) {
        Path name = sourceFile.getFileName();

        return name == null ? sourceFile.toString() : name.toString();
    }

    static String readSource(Path sourceFile) {
        try {
            return Files.readString(sourceFile, StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Could not read " + sourceFile, ioException);
        }
    }

    static List<Path> collectMainSourceFiles(Path serverRoot) {
        List<Path> sourceFiles = new ArrayList<>();

        try {
            Files.walkFileTree(serverRoot, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    Path name = directory.getFileName();

                    if (name != null && SKIPPED_DIRECTORY_NAMES.contains(name.toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    String path = file.toString();

                    if (path.endsWith(JAVA_EXTENSION) && path.contains(mainSourceSegment())) {
                        sourceFiles.add(file);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ioException) {
            throw new UncheckedIOException("Could not walk " + serverRoot, ioException);
        }

        return sourceFiles;
    }

    private static String mainSourceSegment() {
        String separator = File.separator;

        return separator + "src" + separator + "main" + separator + "java" + separator;
    }

    /**
     * The {@code server} directory, found by walking up from the Gradle test working directory (the module directory).
     * Fails loudly rather than silently scanning nothing.
     */
    static Path serverRoot() {
        Path candidate = Paths.get("")
            .toAbsolutePath();

        List<Path> inspected = new ArrayList<>();

        while (candidate != null) {
            inspected.add(candidate);

            Path name = candidate.getFileName();

            if (name != null && "server".equals(name.toString()) && Files.isDirectory(candidate.resolve("libs"))) {
                return candidate;
            }

            candidate = candidate.getParent();
        }

        throw new AssertionError(
            "Could not locate the server source root by walking up from the working directory; inspected " + inspected);
    }
}
