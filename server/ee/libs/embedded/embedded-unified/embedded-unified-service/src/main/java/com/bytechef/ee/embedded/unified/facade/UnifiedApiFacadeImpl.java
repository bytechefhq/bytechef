/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.facade;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter.Page;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.unified.exception.CursorPaginationException;
import com.bytechef.ee.embedded.unified.pagination.CursorPageSlice;
import com.bytechef.ee.embedded.unified.pagination.CursorPageable;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.UnifiedApiDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * Unified API facade implementation that provides CRUD operations for unified API models. Hash comparisons use
 * timing-safe comparison via {@link MessageDigest#isEqual} to prevent timing attacks.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class UnifiedApiFacadeImpl implements UnifiedApiFacade {

    private static final Logger log = LoggerFactory.getLogger(UnifiedApiFacadeImpl.class);

    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final ContextFactory contextFactory;
    private final IntegrationInstanceService integrationInstanceService;
    private final UnifiedApiDefinitionService unifiedApiDefinitionService;

    @SuppressFBWarnings("EI")
    public UnifiedApiFacadeImpl(
        ConnectedUserService connectedUserService, ConnectionService connectionService, ContextFactory contextFactory,
        IntegrationInstanceService integrationInstanceService,
        UnifiedApiDefinitionService unifiedApiDefinitionService) {

        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
        this.integrationInstanceService = integrationInstanceService;
        this.unifiedApiDefinitionService = unifiedApiDefinitionService;
    }

    @Override
    public String create(
        String externalUserId, UnifiedInputModel unifiedInputModel, UnifiedApiCategory category,
        Long integrationInstanceId, Environment environment, ModelType modelType) {

        ComponentConnection componentConnection = getComponentConnection(
            externalUserId, category, integrationInstanceId, environment);

        String componentName = componentConnection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            unifiedApiDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel> providerModelMapper =
            unifiedApiDefinitionService.getUnifiedApiProviderModelMapper(componentName, category, modelType);

        ProviderInputModel providerInputModel = providerModelMapper.desunify(unifiedInputModel, List.of());

        return providerModelAdapter.create(
            providerInputModel, ParametersFactory.create(componentConnection.getParameters()),
            contextFactory.createContext(componentName, componentConnection));
    }

    @Override
    public void delete(
        String externalUserId, String id, UnifiedApiCategory category, Long integrationInstanceId,
        Environment environment, ModelType modelType) {

        ComponentConnection componentConnection = getComponentConnection(
            externalUserId, category, integrationInstanceId, environment);

        String componentName = componentConnection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            unifiedApiDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        providerModelAdapter.delete(
            id, ParametersFactory.create(componentConnection.getParameters()),
            contextFactory.createContext(componentName, componentConnection));
    }

    @Override
    public UnifiedOutputModel get(
        String externalUserId, String id, UnifiedApiCategory category, Long integrationInstanceId,
        Environment environment, ModelType modelType) {

        ComponentConnection componentConnection = getComponentConnection(
            externalUserId, category, integrationInstanceId, environment);

        String componentName = componentConnection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            unifiedApiDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel> providerModelMapper =
            unifiedApiDefinitionService.getUnifiedApiProviderModelMapper(componentName, category, modelType);

        ProviderOutputModel providerOutputModel = providerModelAdapter.get(
            id, ParametersFactory.create(componentConnection.getParameters()),
            contextFactory.createContext(componentName, componentConnection));

        return providerModelMapper.unify(providerOutputModel, List.of());
    }

    @Override
    public CursorPageSlice<? extends UnifiedOutputModel> getPage(
        String externalUserId, CursorPageable cursorPageable, UnifiedApiCategory category, Long integrationInstanceId,
        Environment environment, ModelType modelType) {

        boolean isSorted = StringUtils.hasText(cursorPageable.getSort());

        // Prevent sorting on unindexed fields
//        if (isSorted && !sortableFields.contains(pageRequest.getSort())) {
//            throw new IllegalArgumentException("Sorting is only allowed on fields: " + sortableFields);
//        }

        ComponentConnection componentConnection = getComponentConnection(
            externalUserId, category, integrationInstanceId, environment);

        String componentName = componentConnection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            unifiedApiDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel> providerModelMapper =
            unifiedApiDefinitionService.getUnifiedApiProviderModelMapper(componentName, category, modelType);

        String hashed = getHash(cursorPageable);

        Map<String, ?> cursorParameters = Map.of();

        if (StringUtils.hasText(cursorPageable.getContinuationToken())) {
            String decoded = decrypt(cursorPageable.getContinuationToken());

            log.debug("Decoded continuationToken: {}", decoded);
            String[] params = decoded.split("_");

            String prevHash = params[0];

            // Use timing-safe comparison to prevent timing attacks
            if (!MessageDigest.isEqual(hashed.getBytes(StandardCharsets.UTF_8),
                prevHash.getBytes(StandardCharsets.UTF_8))) {
                throw new CursorPaginationException("Can't modify search filter when using a continuationToken");
            }

            if (params.length != 2 && params.length != 4) {
                throw new CursorPaginationException(
                    "ContinuationToken was expected to have 2 or 4 parts, but got " + params.length);
            }

            if (params.length == 2) {
                cursorParameters = JsonUtils.readMap(params[1]);
            }
        }

        Page<? extends ProviderOutputModel> page = providerModelAdapter.getPage(
            ParametersFactory.create(componentConnection.getParameters()),
            ParametersFactory.create(cursorParameters),
            contextFactory.createContext(componentName, componentConnection));

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
        String externalUserId, String id, UnifiedInputModel unifiedInputModel, UnifiedApiCategory category,
        Long integrationInstanceId, Environment environment, ModelType modelType) {

        ComponentConnection componentConnection = getComponentConnection(
            externalUserId, category, integrationInstanceId, environment);

        String componentName = componentConnection.getComponentName();

        ProviderModelAdapter<? super ProviderInputModel, ? extends ProviderOutputModel> providerModelAdapter =
            unifiedApiDefinitionService.getUnifiedApiProviderModelAdapter(
                componentName, category, modelType);

        ProviderModelMapper<? super UnifiedInputModel, ? extends UnifiedOutputModel, ? extends ProviderInputModel, ? super ProviderOutputModel> providerModelMapper =
            unifiedApiDefinitionService.getUnifiedApiProviderModelMapper(componentName, category, modelType);

        ProviderInputModel providerInputModel = providerModelMapper.desunify(unifiedInputModel, List.of());

        providerModelAdapter.update(
            id, providerInputModel, ParametersFactory.create(componentConnection.getParameters()),
            contextFactory.createContext(componentName, componentConnection));
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

    private ComponentConnection getComponentConnection(
        String externalUserId, UnifiedApiCategory category, Long integrationInstanceId, Environment environment) {

        if (integrationInstanceId == null) {
            List<String> componentNames = unifiedApiDefinitionService.getUnifiedApiComponentDefinitions(category)
                .stream()
                .map(ComponentDefinition::getName)
                .toList();

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                connectedUser.getId(), componentNames, environment);

            integrationInstanceId = integrationInstance.getId();
        }

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        long connectionId = integrationInstance.getConnectionId();

        Connection connection = connectionService.getConnection(connectionId);

        Environment connectionEnvironment = Environment.values()[connection.getEnvironmentId()];

        if (!Objects.equals(connectionEnvironment.name(), environment.name())) {
            throw new ConfigurationException(
                "Connection is not valid for the current environment", ConnectionErrorType.INVALID_CONNECTION);
        }

        return new ComponentConnection(
            connection.getComponentName(), connection.getConnectionVersion(), connectionId,
            connection.getParameters(), connection.getAuthorizationType());
    }

    private String getHash(CursorPageable pageRequest) {
        Sort.Direction direction = pageRequest.getDirection();

        return DigestUtils.md5DigestAsHex((pageRequest.getSort() + direction.name()).getBytes(StandardCharsets.UTF_8));
    }
}
