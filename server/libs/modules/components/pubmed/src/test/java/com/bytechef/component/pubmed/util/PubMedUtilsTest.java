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

package com.bytechef.component.pubmed.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PubMedUtilsTest {

    @Test
    void testFormatDate() {
        assertEquals("2024/03/05", PubMedUtils.formatDate(LocalDate.of(2024, 3, 5)));
        assertNull(PubMedUtils.formatDate(null));
    }

    @Test
    void testGetIdList() {
        assertEquals(
            List.of("1", "2"), PubMedUtils.getIdList(Map.of("esearchresult", Map.of("idlist", List.of("1", "2")))));
        assertEquals(List.of(), PubMedUtils.getIdList(Map.of()));
        assertEquals(List.of(), PubMedUtils.getIdList(null));
    }

    @Test
    void testGetArticlesFallsBackToSourceAndSkipsErrors() {
        Map<String, Object> summaryResponse = Map.of(
            "result", Map.of(
                "1", Map.of("title", "Title 1", "source", "FEBS J", "pubdate", "2020"),
                "2", Map.of("error", "cannot get document summary")));

        Map<String, Object> expectedArticle = new HashMap<>();

        expectedArticle.put("pmid", "1");
        expectedArticle.put("title", "Title 1");
        expectedArticle.put("authors", List.of());
        expectedArticle.put("journal", "FEBS J");
        expectedArticle.put("pubDate", "2020");
        expectedArticle.put("doi", null);
        expectedArticle.put("url", "https://pubmed.ncbi.nlm.nih.gov/1/");

        assertEquals(List.of(expectedArticle), PubMedUtils.getArticles(summaryResponse, List.of("1", "2", "3")));
    }

    @Test
    void testGetArticlesWithoutResult() {
        assertEquals(List.of(), PubMedUtils.getArticles(Map.of(), List.of("1")));
        assertEquals(List.of(), PubMedUtils.getArticles(null, List.of("1")));
    }
}
