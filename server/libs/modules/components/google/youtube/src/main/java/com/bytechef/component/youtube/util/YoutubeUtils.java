package com.bytechef.component.youtube.util;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

/**
 * @author Nikolina Spehar
 */
public class YoutubeUtils {

    public static List<Option<String>> getVideoCategoryIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, Object> response = context.http(http -> http.get("https://www.googleapis.com/youtube/v3/videoCategories"))
            .queryParameters("part", "snippet",
                "regionCode", "US")
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> videoCategoryIdOptions = new ArrayList<>();

        if (response.get("items") instanceof List<?> itemsList) {
            for (Object item : itemsList) {
                if (item instanceof Map<?, ?> itemMap &&
                itemMap.get("snippet") instanceof Map<?, ?> snippetMap) {
                    videoCategoryIdOptions.add(option((String) snippetMap.get("title"), (String) itemMap.get("id")));
                }
            }
        }

        return videoCategoryIdOptions;
    }
}
