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

package com.bytechef.microsoft.commons;

import static com.bytechef.microsoft.commons.MicrosoftConstants.LAST_TIME_CHECKED;
import static com.bytechef.microsoft.commons.MicrosoftConstants.VALUE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MicrosoftTriggerUtils {

    public static PollOutput poll(
        String url, String containsKey, Parameters closureParameters, Context context) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        Map<String, Object> body = context
            .http(http -> http.get(url))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<?, ?>> maps = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map && map.containsKey(containsKey)) {
                    ZonedDateTime zonedCreatedDateTime = ZonedDateTime.parse((String) map.get("createdDateTime"));

                    LocalDateTime createdDateTime = LocalDateTime.ofInstant(zonedCreatedDateTime.toInstant(), zoneId);

                    if (createdDateTime.isAfter(startDate) && createdDateTime.isBefore(now)) {
                        maps.add(map);
                    }
                }
            }
        }

        return new PollOutput(maps, Map.of(LAST_TIME_CHECKED, now), false);
    }
}
