package com.bytechef.platform.ai.repository;

import com.bytechef.platform.ai.domain.WorkflowVector;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowVectorRepository extends ListCrudRepository<WorkflowVector, Long>{

}
