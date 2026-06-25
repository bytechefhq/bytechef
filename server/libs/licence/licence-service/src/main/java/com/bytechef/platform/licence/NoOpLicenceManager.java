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

package com.bytechef.platform.licence;

import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnCEVersion
public class NoOpLicenceManager implements LicenceManager {

    @Override
    public LicenceStatus getStatus() {
        return LicenceStatus.CE;
    }

    @Override
    public Optional<Licence> getLicence() {
        return Optional.empty();
    }

    @Override
    public boolean isFeatureEnabled(LicenceFeature licenceFeature) {
        return false;
    }

    @Override
    public void checkFeature(LicenceFeature licenceFeature) {
        throw new LicenceException("Feature not available in CE edition: " + licenceFeature.getKey());
    }

    @Override
    public long getAllowedJobs() {
        return -1;
    }

    @Override
    public Licence upload(byte[] licenceFileBytes) {
        throw new LicenceException("Licence upload not supported in CE edition");
    }

    @Override
    public void delete() {
        throw new LicenceException("Licence delete not supported in CE edition");
    }
}
