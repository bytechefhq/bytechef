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

package com.bytechef.component.ai.agent.guardrails.util;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.ENTITIES;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.TYPE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.TYPE_ALL;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.TYPE_SELECTED;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.sensitivedata.PiiPatternCatalog;
import com.bytechef.platform.ai.sensitivedata.PiiPatternLabels;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public final class PiiEntityOptions {

    private PiiEntityOptions() {
    }

    public static List<Option<String>> getPiiDetectionOptions() {
        List<String> types = List.of(
            "EMAIL_ADDRESS",
            "PHONE_NUMBER",
            "CREDIT_CARD",
            "IP_ADDRESS",
            "IBAN_CODE",
            "CRYPTO",
            "DATE_TIME",
            "LOCATION",
            "MEDICAL_LICENSE",
            "US_SSN",
            "US_BANK_NUMBER",
            "US_DRIVER_LICENSE",
            "US_ITIN",
            "US_PASSPORT",
            "UK_NHS",
            "UK_NINO",
            "ES_NIF",
            "ES_NIE",
            "IT_FISCAL_CODE",
            "IT_VAT_CODE",
            "IT_DRIVER_LICENSE",
            "IT_PASSPORT",
            "IT_IDENTITY_CARD",
            "PL_PESEL",
            "SG_NRIC_FIN",
            "SG_UEN",
            "AU_ABN",
            "AU_ACN",
            "AU_TFN",
            "AU_MEDICARE",
            "IN_AADHAAR",
            "IN_PAN",
            "IN_PASSPORT",
            "IN_VEHICLE_REGISTRATION",
            "IN_VOTER",
            "FI_PERSONAL_IDENTITY_CODE");

        List<Option<String>> options = new ArrayList<>(types.size());

        for (String type : types) {
            options.add(ComponentDsl.option(PiiPatternLabels.labelOf(type), type));
        }

        return options;
    }

    public static List<PiiPatternCatalog.PiiPattern> selectedPatterns(Parameters parameters) {
        String type = parameters.getString(TYPE, TYPE_ALL);

        if (!TYPE_SELECTED.equals(type)) {
            return PiiPatternCatalog.ALL;
        }

        List<PiiPatternCatalog.PiiPattern> selected =
            PiiPatternCatalog.filterByTypes(parameters.getList(ENTITIES, String.class));

        if (selected.isEmpty()) {
            throw new IllegalArgumentException(
                "PII guardrail TYPE='SELECTED' requires at least one entity in 'Entities'.");
        }

        return selected;
    }
}
