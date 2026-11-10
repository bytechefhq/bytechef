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

import static com.bytechef.component.pubmed.constant.PubMedConstants.AUTHORS;
import static com.bytechef.component.pubmed.constant.PubMedConstants.DOI;
import static com.bytechef.component.pubmed.constant.PubMedConstants.JOURNAL;
import static com.bytechef.component.pubmed.constant.PubMedConstants.PMID;
import static com.bytechef.component.pubmed.constant.PubMedConstants.PUBMED_ARTICLE_URL;
import static com.bytechef.component.pubmed.constant.PubMedConstants.PUB_DATE;
import static com.bytechef.component.pubmed.constant.PubMedConstants.TITLE;
import static com.bytechef.component.pubmed.constant.PubMedConstants.URL;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class PubMedUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private PubMedUtils() {
    }

    public static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    public static List<String> getIdList(Map<String, Object> searchResponse) {
        List<String> ids = new ArrayList<>();

        if (searchResponse != null && searchResponse.get("esearchresult") instanceof Map<?, ?> searchResult &&
            searchResult.get("idlist") instanceof List<?> idList) {

            for (Object id : idList) {
                ids.add(String.valueOf(id));
            }
        }

        return ids;
    }

    public static List<Map<String, Object>> getArticles(Map<String, Object> summaryResponse, List<String> ids) {
        List<Map<String, Object>> articles = new ArrayList<>();

        if (summaryResponse == null || !(summaryResponse.get("result") instanceof Map<?, ?> result)) {
            return articles;
        }

        for (String id : ids) {
            if (result.get(id) instanceof Map<?, ?> summary && !summary.containsKey("error")) {
                articles.add(toArticle(id, summary));
            }
        }

        return articles;
    }

    private static Map<String, Object> toArticle(String pmid, Map<?, ?> summary) {
        Map<String, Object> article = new HashMap<>();

        article.put(PMID, pmid);
        article.put(TITLE, summary.get("title"));
        article.put(AUTHORS, getAuthors(summary));
        article.put(JOURNAL, getJournal(summary));
        article.put(PUB_DATE, summary.get("pubdate"));
        article.put(DOI, getDoi(summary));
        article.put(URL, PUBMED_ARTICLE_URL + pmid + "/");

        return article;
    }

    private static List<String> getAuthors(Map<?, ?> summary) {
        List<String> authors = new ArrayList<>();

        if (summary.get("authors") instanceof List<?> authorList) {
            for (Object author : authorList) {
                if (author instanceof Map<?, ?> authorMap && authorMap.get("name") instanceof String name) {
                    authors.add(name);
                }
            }
        }

        return authors;
    }

    private static Object getDoi(Map<?, ?> summary) {
        if (summary.get("articleids") instanceof List<?> articleIds) {
            for (Object articleId : articleIds) {
                if (articleId instanceof Map<?, ?> articleIdMap && DOI.equals(articleIdMap.get("idtype"))) {
                    return articleIdMap.get("value");
                }
            }
        }

        return null;
    }

    private static Object getJournal(Map<?, ?> summary) {
        Object fullJournalName = summary.get("fulljournalname");

        if (fullJournalName instanceof String journalName && !journalName.isBlank()) {
            return journalName;
        }

        return summary.get("source");
    }
}
