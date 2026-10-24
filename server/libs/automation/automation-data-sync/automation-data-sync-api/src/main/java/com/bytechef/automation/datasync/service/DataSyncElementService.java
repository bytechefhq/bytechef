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

package com.bytechef.automation.datasync.service;

import com.bytechef.automation.datasync.domain.DataSyncElement;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface DataSyncElementService {

    DataSyncElement create(DataSyncElement dataSyncElement);

    void delete(long id);

    Optional<DataSyncElement> fetchDataSyncElement(long id);

    DataSyncElement getDataSyncElement(long id);

    List<DataSyncElement> getByDataSyncId(long dataSyncId);

    DataSyncElement update(DataSyncElement dataSyncElement);
}
