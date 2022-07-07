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

package com.bytechef.hermes.descriptor.ext.service;

import com.bytechef.hermes.descriptor.ext.domain.DescriptorExtHandler;
import com.bytechef.hermes.descriptor.ext.repository.DescriptorExtHandlerRepository;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
public class DescriptorExtHandlerServiceImpl implements DescriptorExtHandlerService {

    private final DescriptorExtHandlerRepository descriptorExtHandlerRepository;

    public DescriptorExtHandlerServiceImpl(DescriptorExtHandlerRepository descriptorExtHandlerRepository) {
        this.descriptorExtHandlerRepository = descriptorExtHandlerRepository;
    }

    @Override
    public Optional<DescriptorExtHandler> fetchExtDescriptorHandler(String name) {
        return Optional.ofNullable(descriptorExtHandlerRepository.findByName(name));
    }

    @Override
    public List<DescriptorExtHandler> getExtDescriptorHandlers(String type) {
        return descriptorExtHandlerRepository.findAllByType(type);
    }

    @Override
    public void save(List<DescriptorExtHandler> descriptorExtHandlers) {
        for (DescriptorExtHandler descriptorExtHandler : descriptorExtHandlers) {
            DescriptorExtHandler oldDescriptorExtHandler =
                    descriptorExtHandlerRepository.findByName(descriptorExtHandler.getName());
            if (oldDescriptorExtHandler == null) {
                descriptorExtHandler.setCreateTime(new Date());
                descriptorExtHandler.setUpdateTime(new Date());

                descriptorExtHandlerRepository.create(descriptorExtHandler);
            } else {
                descriptorExtHandler.setCreateTime(oldDescriptorExtHandler.getCreateTime());
                descriptorExtHandler.setUpdateTime(new Date());

                descriptorExtHandlerRepository.merge(descriptorExtHandler);
            }
        }
    }

    @Override
    public void remove(List<String> names) {
        for (String name : names) {
            descriptorExtHandlerRepository.delete(name);
        }
    }
}
