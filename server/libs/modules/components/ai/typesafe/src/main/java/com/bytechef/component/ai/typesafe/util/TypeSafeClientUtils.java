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

package com.bytechef.component.ai.typesafe.util;

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CONCURRENCY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.JEV_LATEST;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.definition.Parameters;
import org.springaicommunity.typesafe.JevBatchOptions;
import org.springaicommunity.typesafe.TypeSafeClient;

/**
 * @author Ivica Cardic
 */
public final class TypeSafeClientUtils {

    private TypeSafeClientUtils() {
    }

    public static TypeSafeClient createTypeSafeClient(Parameters inputParameters, Parameters connectionParameters) {
        return TypeSafeClient.builder()
            .apiKey(connectionParameters.getRequiredString(TOKEN))
            .defaultModel(inputParameters.getString(MODEL, JEV_LATEST))
            .build();
    }

    public static JevBatchOptions getBatchOptions(Parameters inputParameters) {
        return JevBatchOptions.ofConcurrency(
            inputParameters.getInteger(CONCURRENCY, JevBatchOptions.DEFAULT_CONCURRENCY));
    }
}
