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

package com.bytechef.definition;

/**
 * Represents a reference to a stored file, exposing its identifying metadata such as file name, extension, MIME type,
 * and the URL from which its contents can be retrieved.
 *
 * @author Ivica Cardic
 */
public interface BaseFileEntry {

    /**
     * Returns the file extension of this entry.
     *
     * @return the file extension
     */
    String getExtension();

    /**
     * Returns the MIME type describing the content of this file.
     *
     * @return the MIME type
     */
    String getMimeType();

    /**
     * Returns the name of this file.
     *
     * @return the file name
     */
    String getName();

    /**
     * Returns the URL from which the contents of this file can be retrieved.
     *
     * @return the file URL
     */
    String getUrl();
}
