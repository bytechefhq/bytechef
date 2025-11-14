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

package com.bytechef.component.google.bigquery.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Nikolina Spehar
 */
public class GoogleBigQueryConstants {

    public static final String CREATION_SESSION = "createSession";
    public static final String DRY_RUN = "dryRun";
    public static final String ID = "id";
    public static final String MAX_RESULT = "maxResults";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String PROJECT_ID = "projectId";
    public static final String TIMEOUT_MS = "timeoutMs";
    public static final String QUERY = "query";

    public static final ModifiableObjectProperty TABLE_FIELD_SCHEMA = object()
        .properties(
            string("name")
                .description("The field name."),
            string("type")
                .description("The field data type."),
            string("mode")
                .description("The field mode."),
            array("fields")
                .description("Describes the nested schema fields if the type property is set to RECORD.")
                .items(),
            string("description")
                .description("The field description."),
            object("policyTags")
                .description("The policy tags attached to this field, used for field-level access control.")
                .properties(
                    array("names")
                        .description("A list of policy tag resource names.")
                        .items(string())),
            object("dataPolicies")
                .description("Data policy option.")
                .properties(
                    string("name")
                        .description(
                            "Data policy resource name in the form of " +
                                "projects/projectId/locations/locationId/dataPolicies/data_policy_id.")),
            string("maxLength")
                .description("Maximum length of values of this field for STRINGS or BYTES."),
            string("precision")
                .description(
                    "Precision (maximum number of total digits in base 10) and scale (maximum number of digits in " +
                        "the fractional part in base 10) constraints for values of this field for NUMERIC or " +
                        "BIGNUMERIC."),
            string("scale")
                .description("See documentation for precision."),
            string("roundingMode")
                .description(
                    "Specifies the rounding mode to be used when storing values of NUMERIC and BIGNUMERIC type."),
            string("collation")
                .description("Field collation can be set only when the type of field is STRING."),
            string("defaultValueExpression")
                .description("A SQL expression to specify the default value for this field."),
            object("rangeElementType")
                .description("Represents the type of a field element.")
                .properties(
                    string("type")
                        .description("THe type of a field element.")));

    private GoogleBigQueryConstants() {
    }
}
