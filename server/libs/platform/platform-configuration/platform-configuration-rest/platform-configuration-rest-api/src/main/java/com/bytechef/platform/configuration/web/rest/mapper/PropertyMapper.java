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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.component.registry.domain.ArrayProperty;
import com.bytechef.platform.component.registry.domain.BooleanProperty;
import com.bytechef.platform.component.registry.domain.DateProperty;
import com.bytechef.platform.component.registry.domain.DateTimeProperty;
import com.bytechef.platform.component.registry.domain.DynamicPropertiesProperty;
import com.bytechef.platform.component.registry.domain.FileEntryProperty;
import com.bytechef.platform.component.registry.domain.IntegerProperty;
import com.bytechef.platform.component.registry.domain.NullProperty;
import com.bytechef.platform.component.registry.domain.NumberProperty;
import com.bytechef.platform.component.registry.domain.ObjectProperty;
import com.bytechef.platform.component.registry.domain.OptionsDataSource;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.domain.StringProperty;
import com.bytechef.platform.component.registry.domain.TimeProperty;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import com.bytechef.platform.configuration.web.rest.model.ArrayPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.BooleanPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.DatePropertyModel;
import com.bytechef.platform.configuration.web.rest.model.DateTimePropertyModel;
import com.bytechef.platform.configuration.web.rest.model.DynamicPropertiesPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.FileEntryPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.IntegerPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.NullPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.NumberPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.ObjectPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.OptionsDataSourceModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.bytechef.platform.configuration.web.rest.model.StringPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.TaskPropertyModel;
import com.bytechef.platform.configuration.web.rest.model.TimePropertyModel;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class PropertyMapper {

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class, uses = {
        JsonNullableMapper.class
    })
    public interface ComponentPropertyMapper extends Converter<Property, PropertyModel>, Property.PropertyVisitor {

        @Override
        default PropertyModel convert(Property property) {
            return (PropertyModel) property.accept(this);
        }

        @Override
        default ArrayPropertyModel visit(ArrayProperty arrayProperty) {
            return map(arrayProperty);
        }

        @Override
        default BooleanPropertyModel visit(BooleanProperty booleanProperty) {
            return map(booleanProperty);
        }

        @Override
        default DatePropertyModel visit(DateProperty dateProperty) {
            return map(dateProperty);
        }

        @Override
        default DateTimePropertyModel visit(DateTimeProperty dateTimeProperty) {
            return map(dateTimeProperty);
        }

        @Override
        default DynamicPropertiesPropertyModel visit(DynamicPropertiesProperty dynamicPropertiesProperty) {
            return map(dynamicPropertiesProperty);
        }

        @Override
        default FileEntryPropertyModel visit(FileEntryProperty fileEntryProperty) {
            return map(fileEntryProperty);
        }

        @Override
        default IntegerPropertyModel visit(IntegerProperty integerProperty) {
            return map(integerProperty);
        }

        @Override
        default NullPropertyModel visit(NullProperty nullProperty) {
            return map(nullProperty);
        }

        @Override
        default NumberPropertyModel visit(NumberProperty numberProperty) {
            return map(numberProperty);
        }

        @Override
        default ObjectPropertyModel visit(ObjectProperty objectProperty) {
            return map(objectProperty);
        }

        @Override
        default StringPropertyModel visit(StringProperty stringProperty) {
            return map(stringProperty);
        }

        @Override
        default TimePropertyModel visit(TimeProperty timeProperty) {
            return map(timeProperty);
        }

        ArrayPropertyModel map(ArrayProperty arrayProperty);

        BooleanPropertyModel map(BooleanProperty booleanProperty);

        DatePropertyModel map(DateProperty dateProperty);

        DateTimePropertyModel map(DateTimeProperty dateTimeProperty);

        DynamicPropertiesPropertyModel map(DynamicPropertiesProperty dynamicPropertiesProperty);

        FileEntryPropertyModel map(FileEntryProperty fileEntryProperty);

        IntegerPropertyModel map(IntegerProperty integerProperty);

        NullPropertyModel map(NullProperty nullProperty);

        NumberPropertyModel map(NumberProperty numberProperty);

        ObjectPropertyModel map(ObjectProperty objectProperty);

        OptionsDataSourceModel map(OptionsDataSource optionsDataSource);

        StringPropertyModel map(StringProperty stringProperty);

        TimePropertyModel map(TimeProperty timeProperty);

        default List<PropertyModel> map(List<? extends Property> properties) {
            if (CollectionUtils.isEmpty(properties)) {
                return Collections.emptyList();
            } else {
                return CollectionUtils.map(properties, this::convert);
            }
        }
    }

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class, uses = {
        JsonNullableMapper.class
    })
    public interface TaskDispatcherPropertyMapper
        extends Converter<com.bytechef.platform.workflow.task.dispatcher.registry.domain.Property, PropertyModel>,
        com.bytechef.platform.workflow.task.dispatcher.registry.domain.Property.PropertyVisitor {

        @Override
        default PropertyModel
            convert(com.bytechef.platform.workflow.task.dispatcher.registry.domain.Property property) {
            return (PropertyModel) property.accept(this);
        }

        @Override
        default ArrayPropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.ArrayProperty arrayProperty) {

            return map(arrayProperty);
        }

        @Override
        default BooleanPropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.BooleanProperty booleanProperty) {

            return map(booleanProperty);
        }

        @Override
        default DatePropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.DateProperty dateProperty) {

            return map(dateProperty);
        }

        @Override
        default DateTimePropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.DateTimeProperty dateTimeProperty) {

            return map(dateTimeProperty);
        }

        @Override
        default FileEntryPropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.FileEntryProperty fileEntryProperty) {

            return map(fileEntryProperty);
        }

        @Override
        default IntegerPropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.IntegerProperty integerProperty) {

            return map(integerProperty);
        }

        @Override
        default NullPropertyModel
            visit(com.bytechef.platform.workflow.task.dispatcher.registry.domain.NullProperty nullProperty) {
            return map(nullProperty);
        }

        @Override
        default NumberPropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.NumberProperty numberProperty) {

            return map(numberProperty);
        }

        @Override
        default ObjectPropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.ObjectProperty objectProperty) {

            return map(objectProperty);
        }

        @Override
        default StringPropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.StringProperty stringProperty) {

            return map(stringProperty);
        }

        @Override
        default TaskPropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.TaskProperty taskProperty) {

            return map(taskProperty);
        }

        @Override
        default TimePropertyModel visit(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.TimeProperty timeProperty) {

            return map(timeProperty);
        }

        @Mapping(target = "optionsDataSource", ignore = true)
        ArrayPropertyModel map(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.ArrayProperty arrayProperty);

        BooleanPropertyModel map(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.BooleanProperty booleanProperty);

        @Mapping(target = "optionsDataSource", ignore = true)
        DatePropertyModel map(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.DateProperty dateProperty);

        @Mapping(target = "optionsDataSource", ignore = true)
        DateTimePropertyModel map(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.DateTimeProperty dateTimeProperty);

        FileEntryPropertyModel map(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.FileEntryProperty fileEntryProperty);

        @Mapping(target = "optionsDataSource", ignore = true)
        IntegerPropertyModel map(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.IntegerProperty integerProperty);

        NullPropertyModel map(com.bytechef.platform.workflow.task.dispatcher.registry.domain.NullProperty nullProperty);

        @Mapping(target = "optionsDataSource", ignore = true)
        NumberPropertyModel map(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.NumberProperty numberProperty);

        @Mapping(target = "optionsDataSource", ignore = true)
        ObjectPropertyModel map(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.ObjectProperty objectProperty);

        @Mapping(target = "optionsDataSource", ignore = true)
        StringPropertyModel map(
            com.bytechef.platform.workflow.task.dispatcher.registry.domain.StringProperty stringProperty);

        TaskPropertyModel map(com.bytechef.platform.workflow.task.dispatcher.registry.domain.TaskProperty taskProperty);

        @Mapping(target = "optionsDataSource", ignore = true)
        TimePropertyModel map(com.bytechef.platform.workflow.task.dispatcher.registry.domain.TimeProperty timeProperty);

        default List<PropertyModel> map(
            List<? extends com.bytechef.platform.workflow.task.dispatcher.registry.domain.Property> properties) {

            if (CollectionUtils.isEmpty(properties)) {
                return Collections.emptyList();
            } else {
                return CollectionUtils.map(properties, this::convert);
            }
        }
    }
}
