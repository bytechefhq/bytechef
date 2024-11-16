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

package com.bytechef.platform.util;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WorkflowNodeDescriptionUtils {

    public static String renderComponentProperties(
        Map<String, ?> inputParameters, String componentTile, String operationTitle) {

        StringBuilder sb = new StringBuilder();
        renderProperties(null, inputParameters, sb);

        return """
            <div class="flex flex-col space-y-2">%n\
                <div class="flex flex-col font-semibold">%n\
                     <div class="text-base">%s</div>%n\
                     <div>%s</div>%n\
                </div>%n\
                %s%n\
            </div>%n\
            """.formatted(componentTile, operationTitle, sb);
    }

    public static String renderTaskDispatcherProperties(Map<String, ?> inputParameters, String taskDispatcherTitle) {
        StringBuilder sb = new StringBuilder();

        renderProperties(null, inputParameters, sb);

        return """
            <div class="flex flex-col space-y-2">%n\
                <div class="font-semibold">%n\
                     <div class="text-base">%s</div>%n\
                </div>%n\
                %s%n\
            </div>%n\
            """.formatted(taskDispatcherTitle, ""); // TODO render properties only for the current task dispatcher
    }

    private static void renderProperties(String namePrefix, Map<?, ?> parameterMap, StringBuilder sb) {
        if (parameterMap.isEmpty()) {
            return;
        }

        sb.append("<div class=\"flex flex-col space-y-0.5\">");

        for (Map.Entry<?, ?> entry : parameterMap.entrySet()) {
            String name = (String) entry.getKey();

            if (namePrefix != null) {
                name = namePrefix + "." + name;
            }

            if (entry.getValue() instanceof Map<?, ?> map) {
                renderProperties(name, map, sb);

                continue;
            }

            sb.append("""
                <div class="flex">%n\
                    <div class="w-6/12 text-muted-foreground">%s:</div>%n\
                    <div class="w-6/12">%s</div>%n\
                </div>%n\
                """.formatted(name, entry.getValue()));
        }

        sb.append("</div>");
    }
}
