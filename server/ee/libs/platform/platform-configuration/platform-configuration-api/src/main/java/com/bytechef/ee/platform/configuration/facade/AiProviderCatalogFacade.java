/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import java.util.List;

/**
 * Facade for reading the AI provider catalog. Hosts the {@code USER} authorization guard so it applies to every caller
 * of the facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiProviderCatalogFacade {

    AiDefaultModelDTO getAiDefaultModel(int environment);

    List<AiProviderCatalogItemDTO> getAiProviderCatalog(int environment);
}
