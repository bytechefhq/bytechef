/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.unified.facade;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.UnifiedApiDefinition.Category;
import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter.Page;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.embedded.unified.exception.CursorPaginationException;
import com.bytechef.embedded.unified.pagination.CursorPageSlice;
import com.bytechef.embedded.unified.pagination.CursorPageable;
import com.bytechef.platform.component.definition.ContextFactory;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.domain.ComponentConnection;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionEnvironment;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
public class UnifiedApiFacadeImpl implements UnifiedApiFacade {

    private static final Logger log = LoggerFactory.getLogger(UnifiedApiFacadeImpl.class);

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;
    private final ContextFactory contextFactory;

    @SuppressFBWarnings("EI")
    public UnifiedApiFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService,
        ContextFactory contextFactory) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
    }

    @Override
    public String create(
        UnifiedInputModel unifiedInputModel, Category category, ModelType modelType, ConnectionEnvironment environment,
        long connectionId) {

        ComponentConnection connection = getComponentConnection(connectionId, environment);

        String componentName = connection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            componentDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel> providerModelMapper =
            componentDefinitionService.getUnifiedApiProviderModelMapper(componentName, category, modelType);

        ProviderInputModel providerInputModel = providerModelMapper.desunify(unifiedInputModel, List.of());

        return providerModelAdapter.create(
            providerInputModel, ParametersFactory.createParameters(connection.getParameters()),
            contextFactory.createContext(componentName, connection));
    }

    @Override
    public void delete(
        String id, Category category, ModelType modelType, ConnectionEnvironment environment, long connectionId) {

        ComponentConnection connection = getComponentConnection(connectionId, environment);

        String componentName = connection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            componentDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        providerModelAdapter.delete(
            id, ParametersFactory.createParameters(connection.getParameters()),
            contextFactory.createContext(componentName, connection));
    }

    @Override
    public UnifiedOutputModel get(
        String id, Category category, ModelType modelType, ConnectionEnvironment environment, long connectionId) {

        ComponentConnection connection = getComponentConnection(connectionId, environment);

        String componentName = connection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            componentDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel> providerModelMapper =
            componentDefinitionService.getUnifiedApiProviderModelMapper(componentName, category, modelType);

        ProviderOutputModel providerOutputModel = providerModelAdapter.get(
            id, ParametersFactory.createParameters(connection.getParameters()),
            contextFactory.createContext(componentName, connection));

        return providerModelMapper.unify(providerOutputModel, List.of());
    }

    @Override
    public CursorPageSlice<? extends UnifiedOutputModel> getPage(
        CursorPageable cursorPageable, Category category, ModelType modelType, ConnectionEnvironment environment,
        long connectionId) {

        boolean isSorted = StringUtils.hasText(cursorPageable.getSort());

        // Prevent sorting on unindexed fields
//        if (isSorted && !sortableFields.contains(pageRequest.getSort())) {
//            throw new IllegalArgumentException("Sorting is only allowed on fields: " + sortableFields);
//        }

        ComponentConnection connection = getComponentConnection(connectionId, environment);

        String componentName = connection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            componentDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel> providerModelMapper =
            componentDefinitionService.getUnifiedApiProviderModelMapper(componentName, category, modelType);

        String hashed = getHash(cursorPageable);

        Map<String, ?> cursorParameters = Map.of();

        if (StringUtils.hasText(cursorPageable.getContinuationToken())) {
            String decoded = decrypt(cursorPageable.getContinuationToken());

            log.debug("Decoded continuationToken: {}", decoded);
            String[] params = decoded.split("_");

            String prevHash = params[0];

            if (!hashed.equals(prevHash)) {
                throw new IllegalArgumentException("Can't modify search filter when using a continuationToken");
            }

            if (params.length != 2 && params.length != 4) {
                log.error("Unexpected continuationToken length: {}", params.length);

                throw new CursorPaginationException(
                    "ContinuationToken was expected to have 2 or 4 parts, but got " + params.length);
            }

            if (params.length == 2) {
                cursorParameters = JsonUtils.readMap(params[1]);
            }
        }

        Page<? extends ProviderOutputModel> page = providerModelAdapter.getPage(
            ParametersFactory.createParameters(connection.getParameters()),
            ParametersFactory.createParameters(cursorParameters),
            contextFactory.createContext(componentName, connection));

        String continuationToken = null;

        cursorParameters = page.cursorParameters();

        if (cursorParameters != null && !cursorParameters.isEmpty()) {
            String plainToken = hashed + "_" + JsonUtils.write(page.cursorParameters());

            if (isSorted) {
                Object sortValue = ""; // getValue(persistentEntity.getPersistentProperty(pageRequest.getSort()), last);
                plainToken += "_" + cursorPageable.getSort() + "_" + sortValue;
            }

            if (log.isDebugEnabled()) {
                log.debug("Plain continuationToken: {}", plainToken);
            }

            continuationToken = encrypt(plainToken);
        }

        return new CursorPageSlice<>(
            page.content()
                .stream()
                .map(providerOutputModel -> providerModelMapper.unify(providerOutputModel, List.of()))
                .toList(),
            cursorPageable.getSize(), continuationToken);
    }

    @Override
    public void update(
        String id, UnifiedInputModel unifiedInputModel, Category category, ModelType modelType,
        ConnectionEnvironment environment, long connectionId) {

        ComponentConnection connection = getComponentConnection(connectionId, environment);

        String componentName = connection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            componentDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel> providerModelMapper =
            componentDefinitionService.getUnifiedApiProviderModelMapper(componentName, category, modelType);

        ProviderInputModel providerInputModel = providerModelMapper.desunify(unifiedInputModel, List.of());

        providerModelAdapter.update(
            id, providerInputModel, ParametersFactory.createParameters(connection.getParameters()),
            contextFactory.createContext(componentName, connection));
    }

    private String decrypt(String strToDecrypt) throws CursorPaginationException {
        try {
            byte[] decrypted = EncodingUtils.urlDecodeBase64FromString(strToDecrypt);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting continuationToken: {}", e.getMessage());
            throw new CursorPaginationException("Error decrypting token", e);
        }
    }

    protected String encrypt(String strToEncrypt) throws CursorPaginationException {
        try {
            return EncodingUtils.urlEncodeBase64ToString(strToEncrypt.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error encrypting continuationToken: {}", e.getMessage());

            throw new CursorPaginationException("Error encrypting token", e);
        }
    }

    private ComponentConnection getComponentConnection(long connectionId, ConnectionEnvironment environment) {
        Connection connection = connectionService.getConnection(connectionId);

        if (connection.getEnvironment() != environment) {
            throw new IllegalArgumentException("Connection is not valid for the current environment");
        }

        return new ComponentConnection(
            connection.getComponentName(), connection.getConnectionVersion(), connectionId,
            connection.getParameters(), connection.getAuthorizationName());
    }

    private String getHash(CursorPageable pageRequest) {
        Sort.Direction direction = pageRequest.getDirection();

        return DigestUtils.md5DigestAsHex((pageRequest.getSort() + direction.name()).getBytes(StandardCharsets.UTF_8));
    }
}
