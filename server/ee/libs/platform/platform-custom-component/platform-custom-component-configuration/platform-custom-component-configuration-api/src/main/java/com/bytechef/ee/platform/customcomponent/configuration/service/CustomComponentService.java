/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.service;

import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomComponentService {

    CustomComponent create(CustomComponent customComponent);

    void delete(long id);

    void enableCustomComponent(long id, boolean enable);

    Optional<CustomComponent> fetchCustomComponent(String name, int version);

    CustomComponent getCustomComponent(long id);

    List<CustomComponent> getCustomComponents();

    CustomComponent update(CustomComponent customComponent);
}
