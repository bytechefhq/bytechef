package com.bytechef.helios.project.service;

import com.bytechef.helios.project.domain.ProjectInstanceJob;
import com.bytechef.helios.project.repository.ProjectInstanceJobRepository;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ProjectInstanceJobService {

    private final ProjectInstanceJobRepository projectInstanceJobRepository;

    public ProjectInstanceJobService(ProjectInstanceJobRepository projectInstanceJobRepository) {
        this.projectInstanceJobRepository = projectInstanceJobRepository;
    }

    public ProjectInstanceJob create(long projectInstanceId, long jobId) {
        ProjectInstanceJob projectInstanceJob = new ProjectInstanceJob(projectInstanceId, jobId);

        return null;
    }
}
