/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * The connections and inputs a connected user's reference workflow currently runs with, leaving out the ones its
 * configuration provides.
 *
 * @param connectionIds the connection id of each component, by component name
 * @param inputs        the input values, by input name
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectedUserWorkflowReferenceValuesDTO(Map<String, Long> connectionIds, Map<String, Object> inputs) {
}
