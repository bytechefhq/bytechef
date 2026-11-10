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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.pubmed.constant.PubMedConstants.ABSTRACT;
import static com.bytechef.component.pubmed.constant.PubMedConstants.BASE_URL;
import static com.bytechef.component.pubmed.constant.PubMedConstants.PMID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class PubMedGetArticleAbstractAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getArticleAbstract")
        .title("Get Article Abstract")
        .description("Get the abstract of a PubMed article as plain text.")
        .properties(
            string(PMID)
                .label("PMID")
                .description("PubMed identifier of the article, for example 25315507.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(PMID)
                            .description("PubMed identifier of the article."),
                        string(ABSTRACT)
                            .description(
                                "Abstract of the article in plain text, including the citation, title, authors " +
                                    "and author information."))))
        .help("", "https://docs.bytechef.io/reference/components/pubmed_v1#get-article-abstract")
        .perform(PubMedGetArticleAbstractAction::perform);

    private PubMedGetArticleAbstractAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String pmid = inputParameters.getRequiredString(PMID);

        Object body = context.http(http -> http.get(BASE_URL + "/efetch.fcgi"))
            .queryParameters(
                "db", "pubmed",
                "rettype", "abstract",
                "retmode", "text",
                "id", pmid)
            .configuration(responseType(ResponseType.TEXT))
            .execute()
            .getBody();

        String abstractText = body == null ? "" : String.valueOf(body);

        return Map.of(PMID, pmid, ABSTRACT, abstractText.strip());
    }
}
