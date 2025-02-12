/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository;
import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository.GitWorkflows;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ProjectGitService {

    public GitWorkflows getWorkflows(String url, String branch, String username, String password) {

        GitWorkflowRepository gitWorkflowRepository = new GitWorkflowRepository(url, branch, username, password);

        return gitWorkflowRepository.findAllWithGitInfo();
    }

    public String save(
        List<Workflow> workflows, String commitMessage, String url, String branch, String username, String password) {

        GitWorkflowRepository gitWorkflowRepository = new GitWorkflowRepository(url, branch, username, password);

        return gitWorkflowRepository.save(workflows, commitMessage);
    }
}
