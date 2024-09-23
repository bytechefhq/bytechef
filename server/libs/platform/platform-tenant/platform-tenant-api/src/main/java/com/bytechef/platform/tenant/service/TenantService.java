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

package com.bytechef.platform.tenant.service;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface TenantService {

    String createTenant();

    void deleteTenant(String tenantId);

    String getTenantIdByUserActivationKey(String key);

    List<String> getTenantIdsByUserEmail(String email);

    List<String> getTenantIdsByUserLogin(String login);

    String getTenantIdByUserResetKey(String key);

    List<String> getTenantIds();

    boolean isMultiTenantEnabled();

    void loadChangelog(List<String> tenantIds, String contexts);

    boolean tenantIdsByUserEmailExist(String email);

    boolean tenantIdsByUserLoginExist(String email);
}
