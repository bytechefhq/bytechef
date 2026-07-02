/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection;
import java.util.List;
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
public interface ConnectedUserConnectionRepository extends ListCrudRepository<ConnectedUserConnection, Long> {

    @Query("""
        SELECT connected_user_connection.*
        FROM connected_user_connection
        WHERE connected_user_id = :connectedUserId
        """)
    List<ConnectedUserConnection> findAllByConnectedUserId(@Param("connectedUserId") long connectedUserId);
}
