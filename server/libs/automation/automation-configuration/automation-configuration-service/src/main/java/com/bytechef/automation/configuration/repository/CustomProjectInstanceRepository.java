package com.bytechef.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.platform.constant.Environment;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface CustomProjectInstanceRepository {

    List<ProjectInstance> findAllProjectInstances(Environment environment, Long projectId, Long tagId);
}
