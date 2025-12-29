/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest.accounting;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.unified.accounting.AccountingModelType;
import com.bytechef.component.definition.unified.accounting.model.AccountUnifiedInputModel;
import com.bytechef.ee.embedded.unified.facade.UnifiedApiFacade;
import com.bytechef.ee.embedded.unified.pagination.CursorPageRequest;
import com.bytechef.ee.embedded.unified.pagination.CursorPageSlice;
import com.bytechef.ee.embedded.unified.web.rest.accounting.model.AccountModel;
import com.bytechef.ee.embedded.unified.web.rest.accounting.model.CreateUpdateAccountModel;
import com.bytechef.ee.embedded.unified.web.rest.accounting.model.CreatedModel;
import com.bytechef.ee.embedded.unified.web.rest.accounting.model.ListAccountsPageableParameterModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1/unified/accounting")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AccountingAccountApiController implements AccountApi {

    private final ConversionService conversionService;
    private final UnifiedApiFacade unifiedApiFacade;

    @SuppressFBWarnings("EI")
    public AccountingAccountApiController(ConversionService conversionService, UnifiedApiFacade unifiedApiFacade) {
        this.conversionService = conversionService;
        this.unifiedApiFacade = unifiedApiFacade;
    }

    @Override
    public ResponseEntity<CreatedModel> createAccount(
        CreateUpdateAccountModel createUpdateAccountModel, Long xInstanceId, String environment) {

        return ResponseEntity.ok(
            new CreatedModel(
                unifiedApiFacade.create(
                    OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"),
                    conversionService.convert(createUpdateAccountModel, AccountUnifiedInputModel.class),
                    UnifiedApiDefinition.UnifiedApiCategory.CRM, xInstanceId,
                    Environment.valueOf(StringUtils.upperCase(environment)),
                    AccountingModelType.ACCOUNT)));
    }

    @Override
    public ResponseEntity<AccountModel> getAccount(
        String accountId, Long xInstanceId, String environment, Boolean includeRawData) {

        return ResponseEntity.ok(
            conversionService.convert(
                unifiedApiFacade.get(
                    OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), accountId,
                    UnifiedApiDefinition.UnifiedApiCategory.CRM,
                    xInstanceId, Environment.valueOf(StringUtils.upperCase(environment)), AccountingModelType.ACCOUNT),
                AccountModel.class));
    }

    @Override
    public ResponseEntity<CursorPageSlice> listAccounts(
        Long xInstanceId, String environment, Boolean includeRawData, ListAccountsPageableParameterModel pageable) {

        return ResponseEntity.ok(
            unifiedApiFacade
                .getPage(
                    OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"),
                    conversionService.convert(pageable, CursorPageRequest.class),
                    UnifiedApiDefinition.UnifiedApiCategory.CRM, xInstanceId,
                    Environment.valueOf(StringUtils.upperCase(environment)),
                    AccountingModelType.ACCOUNT)
                .map(unifiedOutputModel -> conversionService.convert(unifiedOutputModel, AccountModel.class)));
    }

    @Override
    public ResponseEntity<Void> updateAccount(
        String accountId, CreateUpdateAccountModel createUpdateAccountModel, Long xInstanceId, String environment) {

        unifiedApiFacade.update(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), accountId,
            conversionService.convert(createUpdateAccountModel, AccountUnifiedInputModel.class),
            UnifiedApiDefinition.UnifiedApiCategory.CRM, xInstanceId,
            Environment.valueOf(StringUtils.upperCase(environment)), AccountingModelType.ACCOUNT);

        return ResponseEntity.noContent()
            .build();
    }
}
