
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.definition;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface Context {

    /**
     *
     * @return
     */
    Optional<Connection> fetchConnection();

    /**
     *
     *
     * @param context
     * @param scope
     * @param scopeId
     * @param key
     * @return
     * @param <T>
     */
    <T> Optional<T> fetchData(String context, int scope, long scopeId, String key);

    /**
     *
     * @return
     */
    Connection getConnection();

    /**
     *
     * @param fileEntry
     * @return
     */
    InputStream getFileStream(FileEntry fileEntry);

    /**
     *
     * @param fileEntry
     * @return
     */
    String readFileToString(FileEntry fileEntry);

    /**
     * @param scope
     * @param scopeId
     * @param key
     * @param data
     */
    void saveData(String context, int scope, long scopeId, String key, Object data);

    /**
     *
     * @param fileName
     * @param data
     * @return
     */
    FileEntry storeFileContent(String fileName, String data);

    /**
     *
     * @param fileName
     * @param inputStream
     * @return
     */
    FileEntry storeFileContent(String fileName, InputStream inputStream);

    /**
     *
     */
    interface Connection {

        /**
         *
         * @return
         */
        Map<String, Object> getParameters();
    }

    /**
     *
     */
    interface FileEntry {

        /**
         *
         * @return
         */
        String getExtension();

        /**
         *
         * @return
         */
        String getMimeType();

        /**
         *
         * @return
         */
        String getName();

        /**
         *
         * @return
         */
        String getUrl();
    }
}
