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

package com.bytechef.component.pubmed.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Ivica Cardic
 */
public class PubMedConstants {

    public static final String ABSTRACT = "abstract";
    public static final String API_KEY = "api_key";
    public static final String AUTHORS = "authors";
    public static final String BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils";
    public static final String DATE_TYPE = "dateType";
    public static final String DOI = "doi";
    public static final String EMAIL = "email";
    public static final String JOURNAL = "journal";
    public static final String MAX_DATE = "maxDate";
    public static final String MAX_RESULTS = "maxResults";
    public static final String MIN_DATE = "minDate";
    public static final String PMID = "pmid";
    public static final String PUB_DATE = "pubDate";
    public static final String PUBMED_ARTICLE_URL = "https://pubmed.ncbi.nlm.nih.gov/";
    public static final String SORT = "sort";
    public static final String TERM = "term";
    public static final String TITLE = "title";
    public static final String TOOL = "tool";
    public static final String URL = "url";

    public static final ModifiableObjectProperty ARTICLE_OUTPUT_PROPERTY = object()
        .properties(
            string(PMID)
                .description("PubMed identifier of the article."),
            string(TITLE)
                .description("Title of the article."),
            array(AUTHORS)
                .description("Names of the article authors.")
                .items(string()),
            string(JOURNAL)
                .description("Full name of the journal the article was published in."),
            string(PUB_DATE)
                .description("Publication date of the article as reported by PubMed."),
            string(DOI)
                .description("Digital Object Identifier of the article, if available."),
            string(URL)
                .description("URL of the article on PubMed."));

    private PubMedConstants() {
    }
}
