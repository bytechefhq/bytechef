/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.ee.platform.configuration.dto.AiProviderDTO;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiProviderFacade {

    void deleteAiProvider(int id);

    List<AiProviderDTO> getAiProviders();

    void updateAiProvider(int id, boolean enabled);

    void updateAiProvider(int id, String apiKey);
}
