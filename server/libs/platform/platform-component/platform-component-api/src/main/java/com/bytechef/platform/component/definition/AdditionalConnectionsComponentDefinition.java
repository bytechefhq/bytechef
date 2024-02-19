package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface AdditionalConnectionsComponentDefinition extends ComponentDefinition {

    /**
     *
     */
    @FunctionalInterface
    interface FilterConnectionDefinitionsFunction {

        /**
         *
         * @param componentDefinition
         * @return
         */
        Optional<ConnectionDefinition> apply(ComponentDefinition componentDefinition);
    }
}
