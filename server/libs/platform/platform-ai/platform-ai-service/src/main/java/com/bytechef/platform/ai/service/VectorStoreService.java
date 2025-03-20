package com.bytechef.platform.ai.service;

import com.bytechef.platform.ai.domain.VectorStore;
import com.bytechef.platform.ai.repository.VectorStoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorStoreService {
    private final VectorStoreRepository repository;

    public VectorStoreService(VectorStoreRepository repository) {
        this.repository = repository;
    }

    public long count() {
        return repository.count();
    }

    public List<VectorStore> findAll() {
        return repository.findAll();
    }
}
