/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.dto;

import java.util.List;

/**
 * The outcome of moving an API collection to a newer project version in place.
 *
 * @param removedEndpointNames  the endpoints deleted because their workflow is absent from the new version
 * @param disabledEndpointNames the endpoints whose workflow was enabled but is carried disabled, because the new
 *                              version needs a connection or input it does not have
 * @param missingConnections    the required connection slots of the new version's workflows that no carried connection
 *                              binds
 * @param missingInputs         the required inputs of the new version's workflows that carry no value
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ApiCollectionVersionUpdateDTO(
    ApiCollectionDTO apiCollection, List<String> removedEndpointNames, List<String> disabledEndpointNames,
    List<MissingConnection> missingConnections, List<MissingInput> missingInputs) {

    public ApiCollectionVersionUpdateDTO {
        removedEndpointNames = List.copyOf(removedEndpointNames);
        disabledEndpointNames = List.copyOf(disabledEndpointNames);
        missingConnections = List.copyOf(missingConnections);
        missingInputs = List.copyOf(missingInputs);
    }

    /**
     * @version ee
     */
    public record MissingConnection(String endpointName, String workflowNodeName, String key, String componentName) {
    }

    /**
     * @version ee
     */
    public record MissingInput(String endpointName, String inputName) {
    }
}
