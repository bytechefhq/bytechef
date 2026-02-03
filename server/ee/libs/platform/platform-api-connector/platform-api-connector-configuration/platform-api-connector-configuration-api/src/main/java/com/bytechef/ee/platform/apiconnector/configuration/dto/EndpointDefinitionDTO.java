/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * DTO for API endpoint definition.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record EndpointDefinitionDTO(
    String path,
    String httpMethod,
    String operationId,
    String summary,
    String description,
    List<ParameterDefinitionDTO> parameters,
    RequestBodyDefinitionDTO requestBody,
    List<ResponseDefinitionDTO> responses) {
}
