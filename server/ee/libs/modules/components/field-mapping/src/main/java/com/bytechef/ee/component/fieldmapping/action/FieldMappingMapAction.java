/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.ee.component.fieldmapping.constant.FieldMappingConstants.DATA;
import static com.bytechef.ee.component.fieldmapping.constant.FieldMappingConstants.INCLUDE_UNMAPPED;
import static com.bytechef.ee.component.fieldmapping.constant.FieldMappingConstants.INPUT_TYPE;
import static com.bytechef.ee.component.fieldmapping.constant.FieldMappingConstants.OBJECT_NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.ee.component.fieldmapping.constant.FieldMappingInputType;
import com.bytechef.ee.component.fieldmapping.mapper.FieldMappingApplier;
import com.bytechef.ee.component.fieldmapping.mapper.FieldMappingDescriptor;
import com.bytechef.ee.component.fieldmapping.mapper.FieldMappingDirection;
import com.bytechef.ee.component.fieldmapping.resolver.FieldMappingDescriptorResolver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class FieldMappingMapAction {

    private FieldMappingMapAction() {
    }

    public static ModifiableActionDefinition of(
        FieldMappingDirection direction, FieldMappingDescriptorResolver resolver) {

        return action(direction.actionName())
            .title(direction.title())
            .description(direction.description())
            .properties(
                string(OBJECT_NAME)
                    .label("Object Name")
                    .description(
                        "The object name declared on the workflow's field mapping input, e.g. \"Contacts\".")
                    .required(true),
                string(INPUT_TYPE)
                    .label("Input Type")
                    .description("Whether Data is a single object or an array of objects.")
                    .options(
                        option("Object", FieldMappingInputType.OBJECT.name()),
                        option("Array", FieldMappingInputType.ARRAY.name()))
                    .required(true),
                object(DATA)
                    .label("Data")
                    .description("The object whose keys are renamed through the mapping.")
                    .displayCondition("%s == '%s'".formatted(INPUT_TYPE, FieldMappingInputType.OBJECT.name()))
                    .required(true),
                array(DATA)
                    .label("Data")
                    .description("The objects whose keys are renamed through the mapping, one by one.")
                    .displayCondition("%s == '%s'".formatted(INPUT_TYPE, FieldMappingInputType.ARRAY.name()))
                    .items(object())
                    .required(true),
                bool(INCLUDE_UNMAPPED)
                    .label("Include Unmapped")
                    .description("Copy source keys that no mapping covers through unchanged. Off by default.")
                    .defaultValue(false))
            .output()
            .perform(
                (PerformFunction) (inputParameters, connectionParameters, context) -> perform(
                    direction, resolver, inputParameters, context));
    }

    public static Object perform(
        FieldMappingDirection direction, FieldMappingDescriptorResolver resolver, Parameters inputParameters,
        ActionContext context) {

        FieldMappingDescriptor descriptor = resolver.resolve(inputParameters.getRequiredString(OBJECT_NAME), context);

        return FieldMappingApplier.apply(
            descriptor, inputParameters.getRequired(DATA), direction,
            inputParameters.getBoolean(INCLUDE_UNMAPPED, false));
    }
}
