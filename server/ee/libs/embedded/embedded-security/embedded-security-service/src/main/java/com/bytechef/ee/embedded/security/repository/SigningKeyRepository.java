/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.repository;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface SigningKeyRepository extends ListCrudRepository<SigningKey, Long> {

    Optional<SigningKey> findByKeyIdAndEnvironment(String keyId, int environment);

    List<SigningKey> findAllByTypeAndEnvironment(int type, int environment);
}
