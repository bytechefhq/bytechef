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

package com.bytechef.platform.apiconnector.file.storage;

import com.bytechef.file.storage.domain.FileEntry;

/**
 * @author Ivica Cardic
 */
public interface ApiConnectorFileStorage {

    void deleteApiConnectorDefinition(FileEntry componentFile);

    void deleteApiConnectorSpecification(FileEntry specificationFile);

    String readApiConnectorDefinition(FileEntry componentFile);

    String readApiConnectorSpecification(FileEntry specificationFile);

    FileEntry storeApiConnectorDefinition(String filename, String definition);

    FileEntry storeApiConnectorSpecification(String filename, String specification);
}
