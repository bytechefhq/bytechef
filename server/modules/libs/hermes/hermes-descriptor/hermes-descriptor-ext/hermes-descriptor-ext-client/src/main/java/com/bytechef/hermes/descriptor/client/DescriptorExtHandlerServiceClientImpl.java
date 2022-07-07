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

package com.bytechef.hermes.descriptor.client;

import com.bytechef.hermes.descriptor.ext.domain.DescriptorExtHandler;
import com.bytechef.hermes.descriptor.ext.service.DescriptorExtHandlerService;
import java.util.List;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ivica Cardic
 */
@Component
public class DescriptorExtHandlerServiceClientImpl implements DescriptorExtHandlerService {

    private static final RestTemplate restTemplate = new RestTemplate();

    private final String extDescriptorRegistrarUrl;

    public DescriptorExtHandlerServiceClientImpl(Environment environment) {
        this.extDescriptorRegistrarUrl = String.format(
                "http://%s:%s/descriptor-ext-handlers",
                environment.getProperty("hermes.controller.host"), environment.getProperty("hermes.controller.port"));
    }

    @Override
    public Optional<DescriptorExtHandler> fetchExtDescriptorHandler(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DescriptorExtHandler> getExtDescriptorHandlers(String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(List<String> names) {
        restTemplate.postForObject(extDescriptorRegistrarUrl + "/remove", names, Void.class);
    }

    @Override
    public void save(List<DescriptorExtHandler> descriptorExtHandlers) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<DescriptorExtHandler>> request = new HttpEntity<>(descriptorExtHandlers, headers);

        restTemplate.postForObject(extDescriptorRegistrarUrl + "/save", request, Void.class);
    }
}
