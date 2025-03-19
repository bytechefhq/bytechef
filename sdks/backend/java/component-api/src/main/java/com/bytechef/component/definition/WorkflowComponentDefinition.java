package com.bytechef.component.definition;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface WorkflowComponentDefinition {

    /**
     *
     * @return
     */
    Optional<List<? extends ActionDefinition>> getActions();

    /**
     *
     * @return
     */
    Optional<Boolean> getCustomAction();

    /**
     *
     * @return
     */
    Optional<Help> getCustomActionHelp();

    /**
     *
     * @return
     */
    Optional<List<? extends TriggerDefinition>> getTriggers();
}
