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

package com.bytechef.component.json.file.constant;

/**
 * @author Ivica Cardic
 */
public class JsonFileConstants {

    public static final String FILE_TYPE = "fileType";
    public static final String IS_ARRAY = "isArray";
    public static final String PATH = "path";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String SOURCE = "source";
    public static final String JSON_FILE = "jsonFile";
    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final String FILE_ENTRY = "fileEntry";
    public static final String FILENAME = "filename";
    public static final String TYPE = "type";

    public enum FileType {
        JSON,
        JSONL,
    }
}
