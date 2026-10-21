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

package com.bytechef.file.storage.teardown;

import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import com.bytechef.tenant.util.TenantIdUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.annotation.Order;

/**
 * @author Ivica Cardic
 */
@Order(1000)
public class FileStorageTenantTeardownContributor implements TenantTeardownContributor {

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public FileStorageTenantTeardownContributor(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void afterSchemaDrop(String tenantId) {
        fileStorageService.deleteTenantFiles(TenantIdUtils.requireValid(tenantId));
    }
}
