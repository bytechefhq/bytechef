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

package com.bytechef.component.pubmed.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.pubmed.constant.PubMedConstants.ARTICLE_OUTPUT_PROPERTY;
import static com.bytechef.component.pubmed.constant.PubMedConstants.BASE_URL;
import static com.bytechef.component.pubmed.constant.PubMedConstants.DATE_TYPE;
import static com.bytechef.component.pubmed.constant.PubMedConstants.MAX_DATE;
import static com.bytechef.component.pubmed.constant.PubMedConstants.MAX_RESULTS;
import static com.bytechef.component.pubmed.constant.PubMedConstants.MIN_DATE;
import static com.bytechef.component.pubmed.constant.PubMedConstants.SORT;
import static com.bytechef.component.pubmed.constant.PubMedConstants.TERM;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.pubmed.util.PubMedUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class PubMedSearchArticlesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchArticles")
        .title("Search Articles")
        .description(
            "Search PubMed for articles matching a query and return their PMIDs with title, authors, journal, " +
                "publication date, DOI and URL.")
        .properties(
            string(TERM)
                .label("Query")
                .description(
                    "PubMed search query. Supports the PubMed search syntax, for example field tags such as " +
                        "[Title], [Author] or [MeSH Terms] and the boolean operators AND, OR and NOT.")
                .required(true),
            integer(MAX_RESULTS)
                .label("Max Results")
                .description("Maximum number of articles to return.")
                .defaultValue(20)
                .minValue(1)
                .maxValue(10000)
                .required(false),
            string(SORT)
                .label("Sort")
                .description("Order in which the matching articles are returned.")
                .options(
                    option("Relevance", "relevance"),
                    option("Publication Date", "pub_date"),
                    option("First Author", "Author"),
                    option("Journal", "JournalName"))
                .defaultValue("relevance")
                .required(false),
            date(MIN_DATE)
                .label("From Date")
                .description(
                    "Return only articles whose date, of the type selected in Date Type, is on or after this date.")
                .required(false),
            date(MAX_DATE)
                .label("To Date")
                .description(
                    "Return only articles whose date, of the type selected in Date Type, is on or before this " +
                        "date.")
                .required(false),
            string(DATE_TYPE)
                .label("Date Type")
                .description("Type of date that From Date and To Date are applied to.")
                .options(
                    option("Publication Date", "pdat"),
                    option("Entrez Date", "edat"),
                    option("Modification Date", "mdat"))
                .defaultValue("pdat")
                .required(false))
        .output(outputSchema(array().items(ARTICLE_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/pubmed_v1#search-articles")
        .perform(PubMedSearchArticlesAction::perform);

    private static final String EARLIEST_DATE = "1000/01/01";
    private static final String LATEST_DATE = "3000/12/31";

    private PubMedSearchArticlesAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String minDate = PubMedUtils.formatDate(inputParameters.getLocalDate(MIN_DATE));
        String maxDate = PubMedUtils.formatDate(inputParameters.getLocalDate(MAX_DATE));
        String dateType = null;

        if (minDate != null || maxDate != null) {
            dateType = inputParameters.getString(DATE_TYPE, "pdat");

            if (minDate == null) {
                minDate = EARLIEST_DATE;
            }

            if (maxDate == null) {
                maxDate = LATEST_DATE;
            }
        }

        Map<String, Object> searchResponse = context.http(http -> http.get(BASE_URL + "/esearch.fcgi"))
            .queryParameters(
                "db", "pubmed",
                "retmode", "json",
                "term", inputParameters.getRequiredString(TERM),
                "retmax", inputParameters.getInteger(MAX_RESULTS, 20),
                "sort", inputParameters.getString(SORT),
                "datetype", dateType,
                "mindate", minDate,
                "maxdate", maxDate)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<String> ids = PubMedUtils.getIdList(searchResponse);

        if (ids.isEmpty()) {
            return List.of();
        }

        Map<String, Object> summaryResponse = context.http(http -> http.get(BASE_URL + "/esummary.fcgi"))
            .queryParameters(
                "db", "pubmed",
                "retmode", "json",
                "id", String.join(",", ids))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return PubMedUtils.getArticles(summaryResponse, ids);
    }
}
