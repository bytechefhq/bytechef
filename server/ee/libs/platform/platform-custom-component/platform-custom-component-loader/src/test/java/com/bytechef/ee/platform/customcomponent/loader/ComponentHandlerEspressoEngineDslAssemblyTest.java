/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentHandlerEspressoEngineDslAssemblyTest {

    private static final String DEFINITION_JSON = """
        {
            "name": "sample",
            "title": "Sample",
            "description": "Sample component",
            "version": 2,
            "actions": [
                {
                    "name": "greet",
                    "title": "Greet",
                    "description": "Greets someone",
                    "properties": [
                        {
                            "name": "name",
                            "type": "STRING",
                            "label": "Name",
                            "required": true,
                            "maxLength": 100
                        },
                        {
                            "name": "mode",
                            "type": "STRING",
                            "label": "Mode",
                            "options": [
                                {"label": "Formal", "value": "formal"},
                                {"label": "Casual", "value": "casual"}
                            ]
                        },
                        {
                            "name": "repeat",
                            "type": "INTEGER",
                            "label": "Repeat",
                            "minValue": 1,
                            "maxValue": 10
                        },
                        {
                            "name": "address",
                            "type": "OBJECT",
                            "label": "Address",
                            "properties": [
                                {"name": "city", "type": "STRING", "label": "City"}
                            ]
                        }
                    ]
                }
            ],
            "triggers": [
                {
                    "name": "onEvent",
                    "title": "On Event",
                    "type": "POLLING",
                    "properties": [
                        {"name": "interval", "type": "INTEGER", "label": "Interval"}
                    ]
                }
            ],
            "unsupported": ["trigger onEvent execution"]
        }
        """;

    @Test
    void testToComponentDefinition() {
        ComponentDefinition componentDefinition = ComponentHandlerEspressoEngine.toComponentDefinition(
            DEFINITION_JSON, Path.of("sample.jar"), "com.example.SampleComponentHandler", "<svg/>");

        assertEquals("sample", componentDefinition.getName());
        assertEquals("Sample", componentDefinition.getTitle()
            .orElse(null));
        assertEquals(2, componentDefinition.getVersion());
        assertEquals("<svg/>", componentDefinition.getIcon()
            .orElse(null));

        List<? extends ActionDefinition> actions = componentDefinition.getActions();

        assertEquals(1, actions.size());

        ActionDefinition actionDefinition = actions.getFirst();

        assertEquals("greet", actionDefinition.getName());
        assertTrue(actionDefinition.getPerform()
            .isPresent());

        List<? extends Property> properties = actionDefinition.getProperties();

        assertEquals(4, properties.size());

        Property.StringProperty nameProperty = (Property.StringProperty) properties.getFirst();

        assertEquals("Name", nameProperty.getLabel()
            .orElse(null));
        assertEquals(Boolean.TRUE, nameProperty.getRequired());
        assertEquals(100, nameProperty.getMaxLength()
            .orElse(null));

        Property.StringProperty modeProperty = (Property.StringProperty) properties.get(1);

        List<? extends Option<String>> options = modeProperty.getOptions();

        assertEquals(2, options.size());
        assertEquals("formal", options.getFirst()
            .getValue());

        Property.IntegerProperty repeatProperty = (Property.IntegerProperty) properties.get(2);

        assertEquals(1L, repeatProperty.getMinValue()
            .orElse(null));
        assertEquals(10L, repeatProperty.getMaxValue()
            .orElse(null));

        Property.ObjectProperty addressProperty = (Property.ObjectProperty) properties.get(3);

        List<? extends Property.ValueProperty<?>> addressChildren = addressProperty.getProperties();

        assertEquals("city", addressChildren.getFirst()
            .getName());

        List<? extends TriggerDefinition> triggers = componentDefinition.getTriggers();

        assertEquals(1, triggers.size());

        TriggerDefinition triggerDefinition = triggers.getFirst();

        assertEquals("onEvent", triggerDefinition.getName());
        assertEquals(TriggerDefinition.TriggerType.POLLING, triggerDefinition.getType());
    }
}
