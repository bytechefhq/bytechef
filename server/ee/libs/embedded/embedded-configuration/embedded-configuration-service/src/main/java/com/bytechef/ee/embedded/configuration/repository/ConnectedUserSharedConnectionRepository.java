/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserSharedConnection;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface ConnectedUserSharedConnectionRepository
    extends ListCrudRepository<ConnectedUserSharedConnection, Long> {

    @Modifying
    @Query("""
        DELETE FROM connected_user_shared_connection
        WHERE connection_id = :connectionId
        """)
    void deleteByConnectionId(@Param("connectionId") long connectionId);

    @Query("""
        SELECT COUNT(*) > 0
        FROM connected_user_shared_connection
        WHERE connection_id = :connectionId
        """)
    boolean existsByConnectionId(@Param("connectionId") long connectionId);
}
