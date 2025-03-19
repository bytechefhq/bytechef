package com.bytechef.component.definition;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ClusterElementComponentDefinition {

    /**
     *
     * @return
     */
    Optional<List<? extends ClusterElementDefinition<?>>> getClusterElements();
}
