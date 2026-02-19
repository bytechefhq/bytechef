/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.facade;

import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomComponentFacade {

    void delete(Long id);

    List<CustomComponent> getCustomComponents();

    void save(byte[] bytes, CustomComponent.Language language);
}
