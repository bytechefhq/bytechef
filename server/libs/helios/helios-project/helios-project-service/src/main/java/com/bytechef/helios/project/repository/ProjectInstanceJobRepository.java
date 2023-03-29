package com.bytechef.helios.project.repository;

import com.bytechef.helios.project.domain.ProjectInstance;
import com.bytechef.helios.project.domain.ProjectInstanceJob;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectInstanceJobRepository extends ListCrudRepository<ProjectInstanceJob, Long> {
}
