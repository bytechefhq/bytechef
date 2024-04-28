/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.twilio.util;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Monika Domiter
 */
public class TwilioUtils {

    private TwilioUtils() {
    }

    public static List<Option<String>> getZoneIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        List<Option<String>> options = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Set<String> zoneIds = ZoneId.getAvailableZoneIds();

        for (String zoneId : zoneIds) {
            if ((zoneId.startsWith("Etc/GMT+") || zoneId.startsWith("Etc/GMT-")) && !zoneId.equals("Etc/GMT-0")) {
                ZonedDateTime zonedDateTime = now.atZone(ZoneId.of(zoneId));

                ZoneOffset zoneOffset = zonedDateTime.getOffset();

                String zoneOffsetId = zoneOffset.getId();

                options.add(ComponentDSL.option("GMT" + zoneOffsetId.replace("Z", "+00:00"), zoneId));
            }
        }

        options.sort((o1, o2) -> {
            String name = o1.getLabel();

            return name.compareTo(o2.getLabel());
        });

        return options;
    }
}
