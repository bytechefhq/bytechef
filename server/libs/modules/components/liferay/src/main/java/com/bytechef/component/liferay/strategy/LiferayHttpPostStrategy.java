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

package com.bytechef.component.liferay.strategy;

import static com.bytechef.component.liferay.constant.LiferayConstants.PARAMETERS;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Nikolina Spehar
 */
public class LiferayHttpPostStrategy implements LiferayHttpStrategy {

    @Override
    public Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context, String serviceEndpoint) {

        return context
            .http(http -> http.post("/api/jsonws%s".formatted(serviceEndpoint)))
            .body(Body.of(inputParameters.getRequiredMap(PARAMETERS), BodyContentType.FORM_DATA))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
