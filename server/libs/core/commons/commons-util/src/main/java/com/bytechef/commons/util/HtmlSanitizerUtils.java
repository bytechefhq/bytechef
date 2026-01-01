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

package com.bytechef.commons.util;

import java.util.Set;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * Utility class for sanitizing HTML and CSS content to prevent XSS attacks.
 *
 * @author Ivica Cardic
 */
public class HtmlSanitizerUtils {

    // Policy for rich text content (allows common formatting)
    private static final PolicyFactory RICH_TEXT_POLICY = new HtmlPolicyBuilder()
        .allowCommonInlineFormattingElements()
        .allowCommonBlockElements()
        .allowElements("span", "div", "p", "br", "strong", "em", "u", "s", "ol", "ul", "li")
        .allowAttributes("class")
        .onElements("span", "div", "p")
        .toFactory();

    // Policy for custom HTML in forms (very restrictive)
    private static final PolicyFactory CUSTOM_HTML_POLICY = new HtmlPolicyBuilder()
        .allowElements("p", "br", "strong", "em", "u", "span")
        .allowTextIn("p", "span", "strong", "em", "u")
        .toFactory();

    // Whitelist of safe CSS properties
    private static final Set<String> SAFE_CSS_PROPERTIES = Set.of(
        "color", "background-color", "background",
        "font-size", "font-weight", "font-family", "font-style",
        "text-align", "text-decoration", "text-transform", "line-height",
        "margin", "margin-top", "margin-bottom", "margin-left", "margin-right",
        "padding", "padding-top", "padding-bottom", "padding-left", "padding-right",
        "border", "border-radius", "border-color", "border-width", "border-style",
        "width", "max-width", "min-width", "height", "max-height", "min-height",
        "display", "opacity", "box-shadow", "text-shadow",
        "letter-spacing", "word-spacing");

    /**
     * Sanitizes HTML content for rich text (descriptions, comments, etc.). Allows common formatting but strips
     * dangerous elements and attributes.
     *
     * @param html the input HTML string to be sanitized; can include markup and text. If null or blank, an empty string
     *             is returned.
     * @return a sanitized version of the input HTML string, ensuring compliance with safety standards.
     */
    public static String sanitizeHtml(String html) {
        if (org.apache.commons.lang3.StringUtils.isBlank(html)) {
            return "";
        }
        return RICH_TEXT_POLICY.sanitize(html);
    }

    /**
     * Sanitizes custom HTML for forms (very restrictive). Only allows basic text formatting, no attributes except basic
     * styling.
     *
     * @param html the input HTML string to be sanitized. It may contain text and markup. If the input is null or blank,
     *             an empty string will be returned.
     * @return a sanitized version of the input HTML string, processed according to the custom HTML policy.
     */
    public static String sanitizeCustomHtml(String html) {
        if (org.apache.commons.lang3.StringUtils.isBlank(html)) {
            return "";
        }
        return CUSTOM_HTML_POLICY.sanitize(html);
    }

    /**
     * Sanitizes CSS styling with a whitelist approach. Blocks dangerous patterns like url(), @import, expression(),
     * etc.
     *
     * @param css the input CSS string to be sanitized. If the input is null or blank, an empty string is returned.
     * @return a sanitized version of the input CSS string, containing only safe and whitelisted CSS properties. If no
     *         valid CSS remains after processing, an empty string will be returned.
     */
    public static String sanitizeCss(String css) {
        if (org.apache.commons.lang3.StringUtils.isBlank(css)) {
            return "";
        }

        // First pass: remove dangerous patterns
        String sanitized = css
            .replaceAll("\\\\.", "") // Remove escapes
            .replaceAll("@[^;{]*[;{]", "") // Remove @-rules
            .replaceAll("url\\s*\\([^)]*\\)", "") // Remove url()
            .replaceAll("expression\\s*\\([^)]*\\)", "") // Remove expression()
            .replaceAll("javascript:", "") // Remove javascript:
            .replaceAll("data:", "") // Remove data:
            .replaceAll("vbscript:", "") // Remove vbscript:
            .replaceAll("-moz-binding", "") // Remove XBL binding
            .replaceAll("behavior\\s*:", ""); // Remove IE behavior

        // Second pass: whitelist properties
        String result = whitelistCssProperties(sanitized);

        // Third pass: remove dangerous values
        return sanitizeCssValues(result);
    }

    /**
     * Sanitizes CSS values to remove dangerous patterns like url(), expression(), etc.
     */
    private static String sanitizeCssValues(String css) {
        if (css == null || css.isEmpty()) {
            return "";
        }

        String[] declarations = css.split("[;\n]");
        StringBuilder result = new StringBuilder();

        for (String declaration : declarations) {
            String trimmed = declaration.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String lowercase = trimmed.toLowerCase();
            if (lowercase.contains("url") ||
                lowercase.contains("expression") ||
                lowercase.contains("javascript:") ||
                lowercase.contains("data:") ||
                lowercase.contains("vbscript:") ||
                lowercase.contains("-moz-binding") ||
                lowercase.contains("behavior") ||
                lowercase.contains("(") ||
                lowercase.contains(")")) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(";\n");
            }
            result.append(trimmed);
        }

        if (!result.isEmpty()) {
            result.append(";");
        }

        return result.toString();
    }

    /**
     * Strips all HTML tags and returns plain text. seful for titles, names, and other fields that should never contain
     * markup.
     *
     * @param html the input string containing HTML content. It may include markup and text elements. If null or blank,
     *             an empty string will be returned.
     * @return a plain text version of the input string with all HTML tags removed, or an empty string if the input is
     *         null or blank.
     */
    public static String stripHtml(String html) {
        if (org.apache.commons.lang3.StringUtils.isBlank(html)) {
            return "";
        }
        return Sanitizers.FORMATTING.sanitize(html)
            .replaceAll("<[^>]*>", "")
            .trim();
    }

    /**
     * Filters CSS to only include whitelisted properties.
     */
    private static String whitelistCssProperties(String css) {
        String[] declarations = css.split("[;\n]");
        StringBuilder result = new StringBuilder();

        for (String declaration : declarations) {
            String trimmed = declaration.trim();
            if (!trimmed.contains(":")) {
                continue;
            }

            String[] parts = trimmed.split(":", 2);
            String property = parts[0].trim()
                .toLowerCase();

            if (SAFE_CSS_PROPERTIES.contains(property)) {
                if (!result.isEmpty()) {
                    result.append(";\n");
                }
                result.append(trimmed);

                if (trimmed.endsWith(";")) {
                    result.deleteCharAt(result.length() - 1);
                }
            }
        }

        if (!result.isEmpty()) {
            result.append(";");
        }

        return result.toString();
    }
}
