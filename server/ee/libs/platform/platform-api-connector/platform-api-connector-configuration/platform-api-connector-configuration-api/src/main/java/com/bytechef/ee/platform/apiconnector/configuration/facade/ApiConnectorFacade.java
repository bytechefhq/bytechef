/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ApiConnectorDTO;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiConnectorFacade {

    boolean cancelGenerationJob(String jobId);

    ApiConnector createApiConnector(ApiConnector apiConnector);

    void deleteApiConnector(long id);

    void enableApiConnector(long id, boolean enable);

    Optional<ApiConnectorGenerationJob> fetchGenerationJob(String jobId);

    ApiConnector generateFromDocumentation(String componentName, String documentationUrl);

    ApiConnector getApiConnector(long id);

    List<ApiConnectorDTO> getApiConnectors();

    ApiConnector importOpenApiSpecification(String componentName, String specification);

    ApiConnectorGenerationJob startGenerationJob(String documentationUrl, String userPrompt);

    ApiConnector updateApiConnector(ApiConnector apiConnector);
}
