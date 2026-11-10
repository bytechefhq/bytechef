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

import static com.bytechef.component.pubmed.constant.PubMedConstants.DATE_TYPE;
import static com.bytechef.component.pubmed.constant.PubMedConstants.MAX_RESULTS;
import static com.bytechef.component.pubmed.constant.PubMedConstants.MIN_DATE;
import static com.bytechef.component.pubmed.constant.PubMedConstants.SORT;
import static com.bytechef.component.pubmed.constant.PubMedConstants.TERM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class PubMedSearchArticlesActionTest {

    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                TERM, "crispr", MAX_RESULTS, 5, SORT, "pub_date", MIN_DATE, LocalDate.of(2024, 1, 15),
                DATE_TYPE, "edat"));

        Map<String, Object> searchResponse = Map.of("esearchresult", Map.of("idlist", List.of("25315507")));
        Map<String, Object> summaryResponse = Map.of(
            "result", Map.of(
                "uids", List.of("25315507"),
                "25315507", Map.of(
                    "title", "Genome modification by CRISPR/Cas9.",
                    "authors", List.of(Map.of("name", "Ma Y"), Map.of("name", "Zhang L")),
                    "fulljournalname", "The FEBS journal",
                    "source", "FEBS J",
                    "pubdate", "2014 Dec",
                    "articleids", List.of(
                        Map.of("idtype", "pubmed", "value", "25315507"),
                        Map.of("idtype", "doi", "value", "10.1111/febs.13110")))));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(searchResponse, summaryResponse);

        List<Map<String, Object>> result = PubMedSearchArticlesAction.perform(mockedParameters, null, mockedContext);

        assertEquals(
            List.of(
                Map.of(
                    "pmid", "25315507",
                    "title", "Genome modification by CRISPR/Cas9.",
                    "authors", List.of("Ma Y", "Zhang L"),
                    "journal", "The FEBS journal",
                    "pubDate", "2014 Dec",
                    "doi", "10.1111/febs.13110",
                    "url", "https://pubmed.ncbi.nlm.nih.gov/25315507/")),
            result);
        assertEquals(
            List.of(
                "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi",
                "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi"),
            stringArgumentCaptor.getAllValues());

        List<Object[]> queryParameters = queryArgumentCaptor.getAllValues();

        assertEquals(
            Arrays.asList(
                "db", "pubmed", "retmode", "json", "term", "crispr", "retmax", 5, "sort", "pub_date", "datetype",
                "edat", "mindate", "2024/01/15", "maxdate", "3000/12/31"),
            Arrays.asList(queryParameters.getFirst()));
        assertEquals(
            List.of("db", "pubmed", "retmode", "json", "id", "25315507"), Arrays.asList(queryParameters.get(1)));

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testPerformWithoutResults(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(TERM, "nothing"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("esearchresult", Map.of("idlist", List.of())));

        List<Map<String, Object>> result = PubMedSearchArticlesAction.perform(mockedParameters, null, mockedContext);

        assertEquals(List.of(), result);
        assertEquals(
            Arrays.asList(
                "db", "pubmed", "retmode", "json", "term", "nothing", "retmax", 20, "sort", null, "datetype", null,
                "mindate", null, "maxdate", null),
            Arrays.asList(queryArgumentCaptor.getValue()));

        verify(mockedHttp, times(1)).get(any(String.class));
    }
}
