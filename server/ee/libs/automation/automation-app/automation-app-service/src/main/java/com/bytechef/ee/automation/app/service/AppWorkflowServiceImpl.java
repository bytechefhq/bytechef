/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppWorkflow;
import com.bytechef.ee.automation.app.domain.AppWorkflowRun;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppWorkflowRepository;
import com.bytechef.ee.automation.app.repository.AppWorkflowRunRepository;
import com.bytechef.ee.automation.app.util.AppBindingKeys;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AppWorkflowServiceImpl implements AppWorkflowService {

    private final AppCallableWorkflowResolver appCallableWorkflowResolver;
    private final AppService appService;
    private final AppWorkflowRepository appWorkflowRepository;
    private final AppWorkflowRunRepository appWorkflowRunRepository;

    @SuppressFBWarnings("EI")
    public AppWorkflowServiceImpl(
        AppCallableWorkflowResolver appCallableWorkflowResolver, AppService appService,
        AppWorkflowRepository appWorkflowRepository, AppWorkflowRunRepository appWorkflowRunRepository) {

        this.appCallableWorkflowResolver = appCallableWorkflowResolver;
        this.appService = appService;
        this.appWorkflowRepository = appWorkflowRepository;
        this.appWorkflowRunRepository = appWorkflowRunRepository;
    }

    @Override
    public AppWorkflow bind(long appId, String workflowKey, long projectDeploymentId, String workflowUuid) {
        if (!AppBindingKeys.isValidKey(workflowKey)) {
            throw new ConfigurationException(
                "Workflow key must match " + AppBindingKeys.keyPattern(), AppErrorType.APP_BINDING_KEY_INVALID);
        }

        checkFlatKeyUnique(appId, workflowKey);

        if (!isUuid(workflowUuid)) {
            throw new ConfigurationException(
                "Workflow uuid '%s' is not a uuid".formatted(workflowUuid), AppErrorType.APP_WORKFLOW_NOT_BINDABLE);
        }

        App app = appService.getApp(appId);

        if (appCallableWorkflowResolver.resolve(
            Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"), app.getEnvironment(), projectDeploymentId,
            workflowUuid)
            .isEmpty()) {

            throw new ConfigurationException(
                ("Workflow %s of project deployment %s cannot be bound to app %s: it must be an enabled Call-workflow "
                    +
                    "workflow of an enabled deployment in the app's workspace and environment")
                        .formatted(workflowUuid, projectDeploymentId, appId),
                AppErrorType.APP_WORKFLOW_NOT_BINDABLE);
        }

        AppWorkflow appWorkflow = appWorkflowRepository.findByAppIdAndWorkflowKey(appId, workflowKey)
            .orElseGet(AppWorkflow::new);

        appWorkflow.setAppId(appId);
        appWorkflow.setProjectDeploymentId(projectDeploymentId);
        appWorkflow.setWorkflowKey(workflowKey);
        appWorkflow.setWorkflowUuid(workflowUuid);

        return appWorkflowRepository.save(appWorkflow);
    }

    @Override
    public AppWorkflowRun createRun(long appId, String workflowKey, long jobId) {
        AppWorkflowRun appWorkflowRun = new AppWorkflowRun();

        appWorkflowRun.setAppId(appId);
        appWorkflowRun.setCreatedDate(Instant.now());
        appWorkflowRun.setJobId(jobId);
        appWorkflowRun.setRunId(UUID.randomUUID()
            .toString());
        appWorkflowRun.setWorkflowKey(workflowKey);

        return appWorkflowRunRepository.save(appWorkflowRun);
    }

    @Override
    public int deleteRunsCreatedBefore(Instant instant) {
        return appWorkflowRunRepository.deleteByCreatedDateBefore(instant);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppWorkflow> fetchBinding(long appId, String workflowKey) {
        return appWorkflowRepository.findByAppIdAndWorkflowKey(appId, workflowKey);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppWorkflowRun> fetchRun(long appId, String runId) {
        return appWorkflowRunRepository.findByAppIdAndRunId(appId, runId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppWorkflow> getBindings(long appId) {
        return appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(appId);
    }

    @Override
    public boolean unbind(long appId, String workflowKey) {
        Optional<AppWorkflow> binding = appWorkflowRepository.findByAppIdAndWorkflowKey(appId, workflowKey);

        binding.ifPresent(appWorkflowRepository::delete);

        return binding.isPresent();
    }

    private void checkFlatKeyUnique(long appId, String workflowKey) {
        String flatKey = AppBindingKeys.toFlatKey(workflowKey);

        boolean collides = appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(appId)
            .stream()
            .map(AppWorkflow::getWorkflowKey)
            .anyMatch(existingKey -> !existingKey.equals(workflowKey) && AppBindingKeys.toFlatKey(existingKey)
                .equals(flatKey));

        if (collides) {
            throw new ConfigurationException(
                "Workflow key '%s' collides with an existing workflow key of the app: both normalize to '%s'"
                    .formatted(workflowKey, flatKey),
                AppErrorType.APP_BINDING_KEY_ALREADY_EXISTS);
        }
    }

    private static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }

        try {
            UUID.fromString(value);

            return value.length() == 36;
        } catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }
}
