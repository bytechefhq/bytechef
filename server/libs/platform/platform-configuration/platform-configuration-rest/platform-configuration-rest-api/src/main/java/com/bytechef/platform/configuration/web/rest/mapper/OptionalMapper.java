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

package com.bytechef.platform.configuration.web.rest.mapper;

import com.bytechef.platform.component.registry.domain.ConnectionDefinitionBasic;
import com.bytechef.platform.component.registry.domain.Help;
import com.bytechef.platform.component.registry.domain.OptionsDataSource;
import com.bytechef.platform.component.registry.domain.Resources;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;

/**
 * @author Ivica Cardic
 */
@Mapper(config = PlatformConfigurationMapperSpringConfig.class)
public interface OptionalMapper {

    default Help mapToHComponentHelp(Optional<Help> value) {
        return value.orElse(null);
    }

    default com.bytechef.platform.component.registry.domain.Property mapToComponentToProperty(
        Optional<com.bytechef.platform.component.registry.domain.Property> value) {

        return value.orElse(null);
    }

    default ConnectionDefinitionBasic mapToConnectionDefinitionBasicDTO(
        Optional<ConnectionDefinitionBasic> value) {

        return value.orElse(null);
    }

    default Object[] mapToArray(Optional<Object[]> value) {
        return value.orElse(null);
    }

    default Boolean mapToBoolean(Optional<Boolean> value) {
        return value.orElse(null);
    }

    default Double mapToDouble(Optional<Double> value) {
        return value.orElse(null);
    }

    default Integer mapToInteger(Optional<Integer> value) {
        return value.orElse(null);
    }

    default List<Object> mapToList(Optional<List<Object>> value) {
        return value.orElse(null);
    }

    default LocalDate mapToLocalDate(Optional<LocalDate> value) {
        return value.orElse(null);
    }

    default LocalDateTime mapToLocalDateTime(Optional<LocalDateTime> value) {
        return value.orElse(null);
    }

    default Long mapToLong(Optional<Long> value) {
        return value.orElse(null);
    }

    default OptionsDataSource mapToOptionsDataSource(Optional<OptionsDataSource> value) {
        return value.orElse(null);
    }

    default Resources mapToComponentResources(Optional<Resources> value) {
        return value.orElse(null);
    }

    default String mapToString(Optional<String> value) {
        return value.orElse(null);
    }

    default String mapToTime(Optional<LocalTime> value) {
        return value.map(LocalTime::toString)
            .orElse(null);
    }

    default com.bytechef.platform.workflow.task.dispatcher.registry.domain.Help mapToTaskDispatcherHelp(
        Optional<com.bytechef.platform.workflow.task.dispatcher.registry.domain.Help> value) {

        return value.orElse(null);
    }

    default com.bytechef.platform.workflow.task.dispatcher.registry.domain.Property mapToTaskDispatcherProperty(
        Optional<com.bytechef.platform.workflow.task.dispatcher.registry.domain.Property> value) {

        return value.orElse(null);
    }

    default com.bytechef.platform.workflow.task.dispatcher.registry.domain.Resources mapToTaskDispatcherResources(
        Optional<com.bytechef.platform.workflow.task.dispatcher.registry.domain.Resources> value) {

        return value.orElse(null);
    }
}
