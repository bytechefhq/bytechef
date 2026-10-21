/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppInstance;
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
    private final AppWorkflowRepository appWorkflowRepository;
    private final AppWorkflowRunRepository appWorkflowRunRepository;

    @SuppressFBWarnings("EI")
    public AppWorkflowServiceImpl(
        AppCallableWorkflowResolver appCallableWorkflowResolver, AppWorkflowRepository appWorkflowRepository,
        AppWorkflowRunRepository appWorkflowRunRepository) {

        this.appCallableWorkflowResolver = appCallableWorkflowResolver;
        this.appWorkflowRepository = appWorkflowRepository;
        this.appWorkflowRunRepository = appWorkflowRunRepository;
    }

    @Override
    public AppWorkflow bind(
        AppInstance appInstance, String workflowKey, long projectDeploymentId, String workflowUuid) {

        long appInstanceId = appInstance.getId();

        if (!AppBindingKeys.isValidKey(workflowKey)) {
            throw new ConfigurationException(
                "Workflow key must match " + AppBindingKeys.keyPattern(), AppErrorType.APP_BINDING_KEY_INVALID);
        }

        checkFlatKeyUnique(appInstanceId, workflowKey);

        if (!isUuid(workflowUuid)) {
            throw new ConfigurationException(
                "Workflow uuid '%s' is not a uuid".formatted(workflowUuid), AppErrorType.APP_WORKFLOW_NOT_BINDABLE);
        }

        if (appCallableWorkflowResolver.resolve(
            appInstance.getWorkspaceId(), appInstance.getEnvironment(), projectDeploymentId, workflowUuid)
            .isEmpty()) {

            throw new ConfigurationException(
                ("Workflow %s of project deployment %s cannot be bound to app instance %s: it must be an enabled "
                    + "Call-workflow workflow of an enabled deployment in the instance's workspace and environment")
                        .formatted(workflowUuid, projectDeploymentId, appInstanceId),
                AppErrorType.APP_WORKFLOW_NOT_BINDABLE);
        }

        AppWorkflow appWorkflow = appWorkflowRepository.findByAppInstanceIdAndWorkflowKey(appInstanceId, workflowKey)
            .orElseGet(AppWorkflow::new);

        appWorkflow.setAppInstanceId(appInstanceId);
        appWorkflow.setProjectDeploymentId(projectDeploymentId);
        appWorkflow.setWorkflowKey(workflowKey);
        appWorkflow.setWorkflowUuid(workflowUuid);

        return appWorkflowRepository.save(appWorkflow);
    }

    @Override
    public AppWorkflowRun createRun(long appInstanceId, String workflowKey, long jobId) {
        AppWorkflowRun appWorkflowRun = new AppWorkflowRun();

        appWorkflowRun.setAppInstanceId(appInstanceId);
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
    public void deleteBindings(long appInstanceId) {
        appWorkflowRepository.deleteAll(appWorkflowRepository.findAllByAppInstanceIdOrderByWorkflowKey(appInstanceId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppWorkflow> fetchBinding(long appInstanceId, String workflowKey) {
        return appWorkflowRepository.findByAppInstanceIdAndWorkflowKey(appInstanceId, workflowKey);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppWorkflowRun> fetchRun(long appInstanceId, String runId) {
        return appWorkflowRunRepository.findByAppInstanceIdAndRunId(appInstanceId, runId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppWorkflow> getBindings(long appInstanceId) {
        return appWorkflowRepository.findAllByAppInstanceIdOrderByWorkflowKey(appInstanceId);
    }

    @Override
    public boolean unbind(long appInstanceId, String workflowKey) {
        Optional<AppWorkflow> binding =
            appWorkflowRepository.findByAppInstanceIdAndWorkflowKey(appInstanceId, workflowKey);

        binding.ifPresent(appWorkflowRepository::delete);

        return binding.isPresent();
    }

    private void checkFlatKeyUnique(long appInstanceId, String workflowKey) {
        String flatKey = AppBindingKeys.toFlatKey(workflowKey);

        boolean collides = appWorkflowRepository.findAllByAppInstanceIdOrderByWorkflowKey(appInstanceId)
            .stream()
            .map(AppWorkflow::getWorkflowKey)
            .anyMatch(existingKey -> !existingKey.equals(workflowKey) && AppBindingKeys.toFlatKey(existingKey)
                .equals(flatKey));

        if (collides) {
            throw new ConfigurationException(
                "Workflow key '%s' collides with an existing workflow key of the app instance: both normalize to '%s'"
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
