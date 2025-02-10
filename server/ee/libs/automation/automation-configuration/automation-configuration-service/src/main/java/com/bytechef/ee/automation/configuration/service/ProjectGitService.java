/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
public class ProjectGitService {

    public List<Workflow> getWorkflows(String url, String branch, String username, String password) {

        GitWorkflowRepository gitWorkflowRepository = new GitWorkflowRepository(url, branch, username, password);

        return gitWorkflowRepository.findAll();
    }

    public void commit(
        List<Workflow> workflows, String commitMessage, String url, String branch, String username, String password) {

        GitWorkflowRepository gitWorkflowRepository = new GitWorkflowRepository(url, branch, username, password);

        gitWorkflowRepository.save(workflows, commitMessage);
    }
}
