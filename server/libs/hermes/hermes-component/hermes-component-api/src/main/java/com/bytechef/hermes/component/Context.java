
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

package com.bytechef.hermes.component;

import com.bytechef.hermes.component.definition.Authorization;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface Context {

    /**
     *
     */
    enum DataStorageScope {
        ACCOUNT(4, "Account"),
        CURRENT_EXECUTION(1, "Current Execution"),
        WORKFLOW(3, "Workflow"),
        WORKFLOW_INSTANCE(2, "Workflow instance");

        private final int id;
        private final String label;

        DataStorageScope(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public static DataStorageScope valueOf(int id) {
            return switch (id) {
                case 1 -> DataStorageScope.CURRENT_EXECUTION;
                case 2 -> DataStorageScope.WORKFLOW_INSTANCE;
                case 3 -> DataStorageScope.WORKFLOW;
                case 4 -> DataStorageScope.ACCOUNT;
                default -> throw new IllegalStateException("Unexpected value: %s".formatted(id));
            };
        }

        public int getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     *
     * @return
     */
    Optional<Connection> fetchConnection();

    /**
     *
     * @param scope
     * @param scopeId
     * @param key
     * @return
     * @param <T>
     */
    <T> Optional<T> fetchValue(DataStorageScope scope, long scopeId, String key);

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
     * @param progress
     */
    void publishActionProgressEvent(int progress);

    /**
     *
     * @param fileEntry
     * @return
     */
    String readFileToString(FileEntry fileEntry);

    /**
     *
     * @param scope
     * @param scopeId
     * @param key
     * @param value
     */
    void saveValue(DataStorageScope scope, long scopeId, String key, Object value);

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
         * @param authorizationContext
         */
        void applyAuthorization(Authorization.AuthorizationContext authorizationContext);

        /**
         *
         * @return
         */
        Optional<String> fetchBaseUri();

        /**
         *
         * @return
         */
        String getBaseUri();

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
