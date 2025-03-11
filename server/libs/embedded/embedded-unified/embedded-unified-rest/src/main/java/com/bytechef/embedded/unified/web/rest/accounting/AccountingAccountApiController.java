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

package com.bytechef.embedded.unified.web.rest.accounting;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.unified.accounting.AccountingModelType;
import com.bytechef.component.definition.unified.accounting.model.AccountUnifiedInputModel;
import com.bytechef.embedded.unified.facade.UnifiedApiFacade;
import com.bytechef.embedded.unified.pagination.CursorPageRequest;
import com.bytechef.embedded.unified.pagination.CursorPageSlice;
import com.bytechef.embedded.unified.web.rest.accounting.model.AccountModel;
import com.bytechef.embedded.unified.web.rest.accounting.model.CreateUpdateAccountModel;
import com.bytechef.embedded.unified.web.rest.accounting.model.CreatedModel;
import com.bytechef.embedded.unified.web.rest.accounting.model.ListAccountsPageableParameterModel;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1/unified/accounting")
@ConditionalOnCoordinator
public class AccountingAccountApiController implements AccountApi {

    private final ConversionService conversionService;
    private final UnifiedApiFacade unifiedApiFacade;

    @SuppressFBWarnings("EI")
    public AccountingAccountApiController(ConversionService conversionService, UnifiedApiFacade unifiedApiFacade) {
        this.conversionService = conversionService;
        this.unifiedApiFacade = unifiedApiFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<CreatedModel> createAccount(
        CreateUpdateAccountModel createUpdateAccountModel, Long xInstanceId, String environment) {

        return ResponseEntity.ok(
            new CreatedModel(
                unifiedApiFacade.create(
                    conversionService.convert(createUpdateAccountModel, AccountUnifiedInputModel.class),
                    UnifiedApiDefinition.UnifiedApiCategory.CRM,
                    AccountingModelType.ACCOUNT, Environment.valueOf(StringUtils.upperCase(environment)),
                    xInstanceId)));
    }

    @Override
    public ResponseEntity<AccountModel> getAccount(
        String accountId, Long xInstanceId, String environment, Boolean includeRawData) {

        return ResponseEntity.ok(
            conversionService.convert(
                unifiedApiFacade.get(
                    accountId, UnifiedApiDefinition.UnifiedApiCategory.CRM, AccountingModelType.ACCOUNT,
                    Environment.valueOf(StringUtils.upperCase(environment)), xInstanceId),
                AccountModel.class));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<CursorPageSlice> listAccounts(
        Long xInstanceId, String environment, Boolean includeRawData, ListAccountsPageableParameterModel pageable) {

        return ResponseEntity.ok(
            unifiedApiFacade
                .getPage(
                    conversionService.convert(pageable, CursorPageRequest.class),
                    UnifiedApiDefinition.UnifiedApiCategory.CRM,
                    AccountingModelType.ACCOUNT, Environment.valueOf(StringUtils.upperCase(environment)),
                    xInstanceId)
                .map(unifiedOutputModel -> conversionService.convert(unifiedOutputModel, AccountModel.class)));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<Void> updateAccount(
        String accountId, CreateUpdateAccountModel createUpdateAccountModel, Long xInstanceId, String environment) {

        unifiedApiFacade.update(
            accountId, conversionService.convert(createUpdateAccountModel, AccountUnifiedInputModel.class),
            UnifiedApiDefinition.UnifiedApiCategory.CRM, AccountingModelType.ACCOUNT,
            Environment.valueOf(StringUtils.upperCase(environment)),
            xInstanceId);

        return ResponseEntity.noContent()
            .build();
    }
}
