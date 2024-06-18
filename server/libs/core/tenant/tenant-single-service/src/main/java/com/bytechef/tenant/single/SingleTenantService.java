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

package com.bytechef.tenant.single;

import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */

@Service
@ConditionalOnProperty(value = "bytechef.tenant.mode", havingValue = "single")
@ConditionalOnCEVersion
public class SingleTenantService implements TenantService {

    @SuppressFBWarnings("EI")
    public SingleTenantService() {
    }

    @Override
    public String createTenant() {
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
    public boolean isMultiTenantEnabled() {
        return false;
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
