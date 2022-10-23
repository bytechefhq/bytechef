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

package com.bytechef.integration.service;

import com.bytechef.integration.domain.Integration;
import com.bytechef.integration.repository.IntegrationRepository;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class IntegrationServiceImpl implements IntegrationService {

    private final IntegrationRepository integrationRepository;

    public IntegrationServiceImpl(IntegrationRepository integrationRepository) {
        this.integrationRepository = integrationRepository;
    }

    @Override
    public Integration add(Integration integration) {
        integration.setId(null);

        return integrationRepository.save(integration);
    }

    @Override
    public void delete(String id) {
        integrationRepository.deleteById(id);
    }

    @Override
    public Integration getIntegration(String id) {
        return integrationRepository.findById(id).orElseThrow();
    }

    @Override
    public List<Integration> getIntegrations() {
        return StreamSupport.stream(integrationRepository.findAll().spliterator(), false)
                .toList();
    }

    @Override
    public Integration update(Integration integration) {
        return integrationRepository.save(integration);
    }
}
