/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter.ScopeData;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class IntegrationInstanceDataStorageCleanup {

    private final ObjectProvider<DataStorage> dataStorageProvider;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final PlatformTransactionManager transactionManager;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceDataStorageCleanup(
        ObjectProvider<DataStorage> dataStorageProvider,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        PlatformTransactionManager transactionManager) {

        this.dataStorageProvider = dataStorageProvider;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.transactionManager = transactionManager;
    }

    public void deleteAfterCommit(long integrationInstanceId) {
        List<ScopeData> scopeDataList = new ArrayList<>();

        scopeDataList.add(
            new ScopeData(DataStorageScope.PRINCIPAL, String.valueOf(integrationInstanceId), PlatformType.EMBEDDED));

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(integrationInstanceId)) {

            scopeDataList.add(
                new ScopeData(
                    DataStorageScope.WORKFLOW, String.valueOf(integrationInstanceWorkflow.getId()),
                    PlatformType.EMBEDDED));
        }

        DataStorageScopeDeleter.deleteAfterCommit(
            dataStorageProvider.getIfAvailable(), transactionManager, scopeDataList);
    }
}
