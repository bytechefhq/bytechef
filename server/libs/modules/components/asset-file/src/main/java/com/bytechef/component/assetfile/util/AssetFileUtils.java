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

package com.bytechef.component.assetfile.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileSource;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ObjectProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared property schemas and DTO conversion for asset file actions.
 *
 * @author Ivica Cardic
 */
public final class AssetFileUtils {

    private AssetFileUtils() {
    }

    /**
     * Returns the public-shape object schema describing an {@link AssetFile} record. Used as the output schema for
     * actions that return a single asset file.
     */
    public static ObjectProperty assetFileSchema() {
        return object()
            .properties(
                integer("id")
                    .label("ID"),
                string("name")
                    .label("Name"),
                string("description")
                    .label("Description"),
                string("mimeType")
                    .label("MIME Type"),
                integer("sizeBytes")
                    .label("Size (bytes)"),
                string("source")
                    .label("Source")
                    .description("Source of the file: USER_UPLOAD or AI_GENERATED."),
                array("tagIds")
                    .label("Tag IDs")
                    .items(integer()),
                dateTime("createdDate")
                    .label("Created Date"),
                string("createdBy")
                    .label("Created By"),
                dateTime("lastModifiedDate")
                    .label("Last Modified Date"),
                string("lastModifiedBy")
                    .label("Last Modified By"));
    }

    /**
     * Returns the items schema for an array output of asset files.
     */
    public static Property.ValueProperty<?> assetFileItemSchema() {
        return assetFileSchema();
    }

    /**
     * Converts an {@link AssetFile} into the public output map shape produced by the actions.
     */
    public static Map<String, Object> toMap(AssetFile assetFile) {
        Map<String, Object> map = new HashMap<>();

        map.put("id", assetFile.getId());
        map.put("name", assetFile.getName());
        map.put("description", assetFile.getDescription());
        map.put("mimeType", assetFile.getMimeType());
        map.put("sizeBytes", assetFile.getSizeBytes());

        AssetFileSource source = assetFile.getSource();

        map.put("source", source == null ? null : source.name());
        map.put("tagIds", assetFile.getTagIds());
        map.put("createdDate", assetFile.getCreatedDate());
        map.put("createdBy", assetFile.getCreatedBy());
        map.put("lastModifiedDate", assetFile.getLastModifiedDate());
        map.put("lastModifiedBy", assetFile.getLastModifiedBy());

        return map;
    }

    /**
     * Converts a list of asset files into output maps.
     */
    public static List<Map<String, Object>> toMaps(List<AssetFile> assetFiles) {
        return assetFiles.stream()
            .map(AssetFileUtils::toMap)
            .toList();
    }
}
