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

package com.bytechef.platform.tenant.single;

import com.bytechef.platform.tenant.annotation.ConditionalOnSingleTenant;
import com.bytechef.platform.tenant.service.TenantService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */

@Service
@ConditionalOnSingleTenant
public class SingleTenantService implements TenantService {

    @Override
    public String createTenant() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTenant(String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTenantIdByUserActivationKey(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getTenantIdsByUserEmail(String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getTenantIdsByUserLogin(String login) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTenantIdByUserResetKey(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getTenantIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMultiTenantEnabled() {
        return false;
    }

    @Override
    public void loadChangelog(List<String> tenantIds, String contexts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tenantIdsByUserEmailExist(String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tenantIdsByUserLoginExist(String email) {
        throw new UnsupportedOperationException();
    }
}
