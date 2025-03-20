package com.bytechef.platform.ai.repository;

import com.bytechef.platform.ai.domain.VectorStore;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VectorStoreRepository extends ListCrudRepository<VectorStore, Long>{

}
