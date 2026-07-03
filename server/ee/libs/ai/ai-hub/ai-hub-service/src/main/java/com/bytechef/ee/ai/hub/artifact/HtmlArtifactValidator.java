/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 * Strict validator for HTML artifacts. Walks the parsed document tree once, accumulating every violation, then throws a
 * single {@link IllegalArgumentException} listing all of them. Single-pass collection lets the LLM fix the whole batch
 * on the next turn instead of round-tripping per rule.
 *
 * <p>
 * Rules pin the structural and "no external refs" invariants the spec requires. The CSP meta tag and the iframe sandbox
 * provide runtime backstops, so a future relaxation here would not silently expose users — but a relaxation should be
 * deliberate and live alongside the rule it weakens.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class HtmlArtifactValidator {

    private HtmlArtifactValidator() {
    }

    static void validate(Document document) {
        List<String> violations = new ArrayList<>();

        checkDoctype(document, violations);
        checkRequiredElements(document, violations);
        checkScripts(document, violations);
        checkLinkRels(document, violations);
        checkMediaSources(document, violations);
        checkNestedBrowsingContexts(document, violations);
        checkFormActions(document, violations);
        checkBaseTag(document, violations);

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                "HTML artifact failed validation:\n  - " + String.join("\n  - ", violations));
        }
    }

    private static void checkDoctype(Document document, List<String> violations) {
        boolean hasHtmlDoctype = false;

        for (Node node : document.childNodes()) {
            if (node instanceof DocumentType doctype && "html".equalsIgnoreCase(doctype.name())) {
                hasHtmlDoctype = true;

                break;
            }
        }

        if (!hasHtmlDoctype) {
            violations.add("missing <!doctype html> declaration");
        }
    }

    private static void checkRequiredElements(Document document, List<String> violations) {
        if (document.selectFirst("html") == null) {
            violations.add("missing <html> element");
        }

        if (document.head() == null) {
            violations.add("missing <head> element");
        }

        if (document.body() == null) {
            violations.add("missing <body> element");
        }
    }

    private static void checkScripts(Document document, List<String> violations) {
        for (Element script : document.select("script[src]")) {
            String src = script.attr("src");

            if (!isAllowedAssetUri(src)) {
                violations.add(
                    "external script src not allowed: '" + truncate(src) + "' (use inline <script> or data: URI)");
            }
        }
    }

    private static void checkLinkRels(Document document, List<String> violations) {
        for (Element link : document.select("link")) {
            String rel = link.attr("rel")
                .toLowerCase(Locale.ROOT);
            String href = link.attr("href");

            if ("stylesheet".equals(rel)) {
                if (!isAllowedAssetUri(href)) {
                    violations.add(
                        "external stylesheet not allowed: '" + truncate(href) + "' (use inline <style> or data: URI)");
                }
            } else if (!rel.isBlank()) {
                violations.add("<link rel=\"" + rel + "\"> not allowed (only stylesheet links permitted)");
            }
        }
    }

    private static void checkMediaSources(Document document, List<String> violations) {
        String[] tags = {
            "img", "source", "video", "audio", "track"
        };

        for (String tag : tags) {
            for (Element element : document.select(tag)) {
                checkMediaAttribute(element, "src", "external " + label(tag) + " src", violations);
                checkSrcset(element, "external " + label(tag) + " srcset", violations);
                checkMediaAttribute(element, "poster", "external " + label(tag) + " poster", violations);
            }
        }
    }

    private static void checkMediaAttribute(
        Element element, String attribute, String label, List<String> violations) {

        if (!element.hasAttr(attribute)) {
            return;
        }

        String value = element.attr(attribute);

        if (!isAllowedAssetUri(value)) {
            violations.add(label + " not allowed: '" + truncate(value) + "' (use data: URI)");
        }
    }

    /**
     * Validates a {@code srcset} attribute. Unlike {@code src}, {@code srcset} is a comma-separated list of candidates
     * of the form {@code <url> <descriptor>?} (the descriptor is optional, e.g. {@code 2x} or {@code 320w}). A naive
     * prefix check on the whole attribute can pass a value like {@code "data:image/gif;base64,abc 1x,
     * https://tracker.example/img.jpg 2x"} because it leads with {@code data:} — but the second candidate is an
     * external URL. We split on commas only when they are followed by whitespace (the RFC 7903 candidate separator
     * form), which avoids breaking on commas embedded inside {@code data:} URIs (the one after {@code base64} is
     * immediately followed by the base64 payload, not whitespace). Each extracted URL token is then validated
     * independently so the validator's "no external refs" invariant holds even on multi-entry srcsets.
     */
    private static void checkSrcset(Element element, String label, List<String> violations) {
        if (!element.hasAttr("srcset")) {
            return;
        }

        String value = element.attr("srcset");

        if (value.trim()
            .isEmpty()) {
            return;
        }

        // Split on comma-whitespace boundaries to avoid breaking data: URIs which contain commas internally
        // (e.g. "data:image/gif;base64,abc"). In a valid srcset, the candidate separator is always a comma
        // followed by at least one whitespace character; a comma inside a data: URI payload is never followed
        // immediately by whitespace.
        List<String> candidates = splitSrcsetCandidates(value);

        for (String candidate : candidates) {
            String trimmed = candidate.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            // The descriptor (e.g. "2x" or "320w") is whitespace-separated from the URL. Take only the leading
            // token. URLs cannot themselves contain unescaped whitespace, so this split is safe.
            int firstWhitespace = indexOfWhitespace(trimmed);

            String url = firstWhitespace < 0 ? trimmed : trimmed.substring(0, firstWhitespace);

            if (!isAllowedAssetUri(url)) {
                violations.add(label + " not allowed: '" + truncate(url) + "' (use data: URI)");
            }
        }
    }

    /**
     * Splits a {@code srcset} value into candidate strings using comma-followed-by-whitespace as the delimiter. Commas
     * inside {@code data:} URI payloads (e.g. after {@code base64,}) are never immediately followed by a whitespace
     * character in well-formed srcset values, so this heuristic correctly preserves them.
     */
    private static List<String> splitSrcsetCandidates(String srcset) {
        List<String> candidates = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < srcset.length(); i++) {
            // A srcset candidate separator is a comma followed by at least one whitespace character.
            // Commas embedded in data: URIs (e.g. "base64,abc") are followed by non-whitespace.
            if (srcset.charAt(i) == ',' && i + 1 < srcset.length() && isWhitespace(srcset.charAt(i + 1))) {
                candidates.add(srcset.substring(start, i));
                start = i + 1;
            }
        }

        candidates.add(srcset.substring(start));

        return candidates;
    }

    private static boolean isWhitespace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f';
    }

    private static int indexOfWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (isWhitespace(value.charAt(i))) {
                return i;
            }
        }

        return -1;
    }

    private static void checkNestedBrowsingContexts(Document document, List<String> violations) {
        Elements nested = document.select("iframe, frame, frameset, embed, object, applet");

        if (!nested.isEmpty()) {
            violations.add(
                "nested browsing context tags not allowed: " + nested.size()
                    + " element(s) found (iframe/frame/frameset/embed/object/applet)");
        }
    }

    private static void checkFormActions(Document document, List<String> violations) {
        for (Element form : document.select("form[action]")) {
            String action = form.attr("action");

            if (isAbsoluteUrl(action)) {
                violations.add("external form action not allowed: '" + truncate(action) + "'");
            }
        }
    }

    private static void checkBaseTag(Document document, List<String> violations) {
        if (!document.select("base")
            .isEmpty()) {
            violations.add("<base> tag not allowed (would re-anchor relative URLs)");
        }
    }

    private static boolean isAllowedAssetUri(String value) {
        if (value == null) {
            return true;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return true;
        }

        return trimmed.toLowerCase(Locale.ROOT)
            .startsWith("data:");
    }

    private static boolean isAbsoluteUrl(String value) {
        if (value == null) {
            return false;
        }

        String trimmed = value.trim()
            .toLowerCase(Locale.ROOT);

        return trimmed.startsWith("http://") || trimmed.startsWith("https://")
            || trimmed.startsWith("//");
    }

    private static String label(String tag) {
        return switch (tag) {
            case "img" -> "image";
            case "source" -> "media source";
            case "video" -> "video";
            case "audio" -> "audio";
            case "track" -> "track";
            default -> tag;
        };
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }

        return value.length() > 80 ? value.substring(0, 77) + "..." : value;
    }
}
