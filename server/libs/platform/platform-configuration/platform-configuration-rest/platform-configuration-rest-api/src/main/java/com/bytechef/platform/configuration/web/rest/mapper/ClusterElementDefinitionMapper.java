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

package com.bytechef.platform.configuration.web.rest.mapper;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import com.bytechef.platform.configuration.web.rest.model.ClusterElementDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ClusterElementDefinitionModel;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ClusterElementDefinitionMapper {

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class)
    public interface ClusterElementDefinitionToClusterElementDefinitionModelMapper
        extends Converter<ClusterElementDefinition, ClusterElementDefinitionModel> {

        @AfterMapping
        default void afterMapping(
            ClusterElementDefinition clusterElementDefinition,
            @MappingTarget ClusterElementDefinitionModel clusterElementDefinitionModel) {

            clusterElementDefinitionModel.setIcon(
                "/icons/%s.svg".formatted(clusterElementDefinition.getComponentName()));
        }

        @Override
        ClusterElementDefinitionModel convert(ClusterElementDefinition clusterElementDefinition);

        default String map(ClusterElementType clusterElementType) {
            return clusterElementType.name();
        }
    }

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class)
    public interface ClusterElementDefinitionToClusterElementDefinitionBasicModelMapper
        extends Converter<ClusterElementDefinition, ClusterElementDefinitionBasicModel> {

        @AfterMapping
        default void afterMapping(
            ClusterElementDefinition clusterElementDefinition,
            @MappingTarget ClusterElementDefinitionBasicModel clusterElementDefinitionBasicModel) {

            clusterElementDefinitionBasicModel.setIcon(
                "/icons/%s.svg".formatted(clusterElementDefinition.getComponentName()));
        }

        @Override
        ClusterElementDefinitionBasicModel convert(ClusterElementDefinition clusterElementDefinition);

        default String map(ClusterElementType clusterElementType) {
            return clusterElementType.name();
        }
    }
}
