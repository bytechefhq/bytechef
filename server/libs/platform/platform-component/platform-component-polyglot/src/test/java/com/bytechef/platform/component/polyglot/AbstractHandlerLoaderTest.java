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

package com.bytechef.platform.component.polyglot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cache.CacheManager;

/**
 * @author Ivica Cardic
 */
class AbstractHandlerLoaderTest {

    private static final TestHandlerLoader TEST_HANDLER_LOADER = new TestHandlerLoader();

    @TempDir
    Path tempDirectory;

    @Test
    void testJavaLanguageLoadsTheJavaHandler() throws Exception {
        URL url = writeScript("ignored");

        assertEquals("java:cacheKey", TEST_HANDLER_LOADER.loadHandler(url, TestLanguage.JAVA, "cacheKey", null));
    }

    @Test
    void testScriptLanguagesLoadThePolyglotHandlerWithTheLanguageId() throws Exception {
        URL url = writeScript("script source");

        assertEquals(
            "js:script source", TEST_HANDLER_LOADER.loadHandler(url, TestLanguage.JAVASCRIPT, "cacheKey", null));
        assertEquals(
            "python:script source", TEST_HANDLER_LOADER.loadHandler(url, TestLanguage.PYTHON, "cacheKey", null));
    }

    @Test
    void testUnsupportedLanguageIsRejected() throws Exception {
        URL url = writeScript("puts 'hello'");

        RuntimeException exception = assertThrows(
            RuntimeException.class, () -> TEST_HANDLER_LOADER.loadHandler(url, TestLanguage.RUBY, "cacheKey", null));

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    private URL writeScript(String script) throws Exception {
        Path scriptPath = tempDirectory.resolve("handler.txt");

        Files.writeString(scriptPath, script);

        return scriptPath.toUri()
            .toURL();
    }

    private enum TestLanguage {
        JAVA, JAVASCRIPT, PYTHON, RUBY
    }

    private static class TestHandlerLoader extends AbstractHandlerLoader<String> {

        @Override
        protected String loadJavaHandler(URL url, String cacheKey, CacheManager cacheManager) {
            return "java:" + cacheKey;
        }

        @Override
        protected String loadPolyglotHandler(String languageId, String script) {
            return languageId + ":" + script;
        }
    }
}
