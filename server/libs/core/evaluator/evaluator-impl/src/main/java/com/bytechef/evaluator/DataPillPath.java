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

package com.bytechef.evaluator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings(
    value = "REDOS",
    justification = "Each repetition in the pill grammar starts with a '.' or '[' delimiter, so a path matches one way")
record DataPillPath(String path) {

    private static final String INDEX_REGEX = "\\[(?:\\d+|'[a-zA-Z0-9_\\- \\p{L}]*')]";
    private static final String NAME_REGEX = "[a-zA-Z_][a-zA-Z0-9_]*";

    static final String PATH_REGEX = NAME_REGEX + "(?:" + INDEX_REGEX + ")*(?:\\.(?:" + NAME_REGEX + "(?:" +
        INDEX_REGEX + ")*|" + INDEX_REGEX + "))*";

    private static final Pattern PATH_PATTERN = Pattern.compile(PATH_REGEX);
    private static final Pattern SEGMENT_PATTERN = Pattern.compile(
        "\\.?\\[(\\d{1,9})]|\\.?\\['([^']*)']|\\.?(" + NAME_REGEX + ")");

    DataPillPath {
        path = path.strip();
    }

    @Nullable
    Object resolve(@Nullable Object root) {
        Matcher pathMatcher = PATH_PATTERN.matcher(path);

        if (!pathMatcher.matches()) {
            throw new IllegalArgumentException(
                "Unsupported data pill ${" + path + "}: use names, [index] and ['key'] joined by dots, e.g. " +
                    "${judge.answers[0]['first name']}");
        }

        Matcher matcher = SEGMENT_PATTERN.matcher(path);

        Object value = root;
        int position = 0;

        while (position < path.length()) {
            if (!matcher.find(position) || matcher.start() != position) {
                return null;
            }

            String indexGroup = matcher.group(1);

            if (indexGroup != null) {
                int index = Integer.parseInt(indexGroup);

                if (!(value instanceof List<?> list) || index >= list.size()) {
                    return null;
                }

                value = list.get(index);
            } else {
                String quotedKey = matcher.group(2);

                String key = quotedKey == null ? matcher.group(3) : quotedKey;

                if (!(value instanceof Map<?, ?> map) || !map.containsKey(key)) {
                    return null;
                }

                value = map.get(key);
            }

            position = matcher.end();
        }

        return value;
    }
}
