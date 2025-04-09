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

package com.bytechef.component.supabase.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class SupabaseUtils {

    public static List<Option<String>> getBucketNameOptions(
        Parameters parameters, Parameters parameters1, Map<String, String> stringStringMap, String s, Context context) {

        Object[] buckets = context.http(http -> http.get("/storage/v1/bucket/"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> bucketOptions = new ArrayList<>();

        for (Object bucket : buckets) {
            if (bucket instanceof Map<?, ?> bucketMap) {
                bucketOptions.add(option((String) bucketMap.get("name"), (String) bucketMap.get("name")));
            }
        }

        return bucketOptions;
    }
}
