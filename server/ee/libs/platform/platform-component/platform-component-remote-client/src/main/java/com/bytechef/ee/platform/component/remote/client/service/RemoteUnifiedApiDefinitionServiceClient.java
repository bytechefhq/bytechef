/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.UnifiedApiDefinitionService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteUnifiedApiDefinitionServiceClient implements UnifiedApiDefinitionService {

    @Override
    public List<ComponentDefinition> getUnifiedApiComponentDefinitions(UnifiedApiCategory category) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel>
        getUnifiedApiProviderModelAdapter(
            String componentName, UnifiedApiCategory category, ModelType modelType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public
        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel>
        getUnifiedApiProviderModelMapper(
            String componentName, UnifiedApiCategory category, ModelType modelType) {

        throw new UnsupportedOperationException();
    }
}
