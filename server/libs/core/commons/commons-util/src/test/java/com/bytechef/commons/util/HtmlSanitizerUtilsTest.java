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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class HtmlSanitizerUtilsTest {

    @Test
    public void testSanitizeHtml() {
        assertEquals("", HtmlSanitizerUtils.sanitizeHtml(null));
        assertEquals("", HtmlSanitizerUtils.sanitizeHtml(""));
        assertEquals("", HtmlSanitizerUtils.sanitizeHtml("   "));

        String input = "<p>Hello <strong>World</strong> <script>alert('XSS')</script></p>";
        String expected = "<p>Hello <strong>World</strong> </p>";
        assertEquals(expected, HtmlSanitizerUtils.sanitizeHtml(input));

        input = "<span class=\"my-class\" onclick=\"alert(1)\">Text</span>";
        expected = "<span class=\"my-class\">Text</span>";
        assertEquals(expected, HtmlSanitizerUtils.sanitizeHtml(input));

        input = "<div><p>Line 1</p><br><ul><li>Item 1</li></ul></div>";
        expected = "<div><p>Line 1</p><br /><ul><li>Item 1</li></ul></div>";
        assertEquals(expected, HtmlSanitizerUtils.sanitizeHtml(input));
    }

    @Test
    public void testSanitizeCustomHtml() {
        assertEquals("", HtmlSanitizerUtils.sanitizeCustomHtml(null));
        assertEquals("", HtmlSanitizerUtils.sanitizeCustomHtml(""));

        String input = "<p>Hello <strong>World</strong></p><div class=\"test\">Div</div>";
        String expected = "<p>Hello <strong>World</strong></p>Div";
        assertEquals(expected, HtmlSanitizerUtils.sanitizeCustomHtml(input));

        input = "<span>Text</span><br><script>alert(1)</script>";
        expected = "Text<br />";
        assertEquals(expected, HtmlSanitizerUtils.sanitizeCustomHtml(input));
    }

    @Test
    public void testSanitizeCss() {
        assertEquals("", HtmlSanitizerUtils.sanitizeCss(null));
        assertEquals("", HtmlSanitizerUtils.sanitizeCss(""));

        String input = "color: red; background-image: url('test.png'); font-size: 12px;";
        String expected = "color: red;\nfont-size: 12px;";
        assertEquals(expected, HtmlSanitizerUtils.sanitizeCss(input));

        input = "margin: 10px; @import 'style.css'; padding: 5px;";
        expected = "margin: 10px;\npadding: 5px;";
        assertEquals(expected, HtmlSanitizerUtils.sanitizeCss(input));

        input = "display: block; position: absolute; opacity: 0.5;";
        expected = "display: block;\nopacity: 0.5;";
        assertEquals(expected, HtmlSanitizerUtils.sanitizeCss(input));

        input = "color: expression(alert('XSS')); background: javascript:alert(1);";
        expected = "";
        assertEquals(expected, HtmlSanitizerUtils.sanitizeCss(input));
    }

    @Test
    public void testStripHtml() {
        assertEquals("", HtmlSanitizerUtils.stripHtml(null));
        assertEquals("", HtmlSanitizerUtils.stripHtml(""));

        String input = "<p>Hello <b>World</b></p>";
        String expected = "Hello World";
        assertEquals(expected, HtmlSanitizerUtils.stripHtml(input));

        input = "  <div>Text with <br> break</div>  ";
        expected = "Text with  break";
        assertEquals(expected, HtmlSanitizerUtils.stripHtml(input));
    }
}
