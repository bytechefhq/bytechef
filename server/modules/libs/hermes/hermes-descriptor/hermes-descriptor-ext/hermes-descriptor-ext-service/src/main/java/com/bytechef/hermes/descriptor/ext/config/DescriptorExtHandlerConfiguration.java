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

package com.bytechef.hermes.descriptor.ext.config;

import com.bytechef.hermes.descriptor.ext.repository.DescriptorExtHandlerRepository;
import com.bytechef.hermes.descriptor.ext.service.DescriptorExtHandlerService;
import com.bytechef.hermes.descriptor.ext.service.DescriptorExtHandlerServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Ivica Caardic
 */
@Configuration
public class DescriptorExtHandlerConfiguration {

    @Bean
    DescriptorExtHandlerRepository extDescriptorHandlerRepository(
            NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {

        return new DescriptorExtHandlerRepository(jdbcTemplate, objectMapper);
    }

    @Bean
    DescriptorExtHandlerService extDescriptorHandlerService(
            DescriptorExtHandlerRepository descriptorExtHandlerRepository) {
        return new DescriptorExtHandlerServiceImpl(descriptorExtHandlerRepository);
    }
}
