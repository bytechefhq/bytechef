/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Nikolina Spehar
 */
@Service
public class ConnectionLifecycleFacadeImpl implements ConnectionLifecycleFacade {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionLifecycleFacadeImpl.class);

    private final ConnectionRefreshScheduler connectionRefreshScheduler;

    @SuppressFBWarnings("EI")
    public ConnectionLifecycleFacadeImpl(ConnectionRefreshScheduler connectionRefreshScheduler) {
        this.connectionRefreshScheduler = connectionRefreshScheduler;
    }

    @Override
    public void scheduleConnectionRefresh(
        Long connectionId, Map<String, ?> parameters, AuthorizationType authorizationType) {

        try {
            if (authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE ||
                authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {

                Integer expiresIn = (Integer) parameters.get("expires_in");

                Instant expiry = Instant.now()
                    .plusSeconds(expiresIn);

                if (expiry != null) {
                    connectionRefreshScheduler.scheduleConnectionRefresh(connectionId, expiry);
                }
            }
        } catch (Exception e) {
            logger.debug(
                "Connection creation error");
        }
    }

    @Override
    public void deleteScheduledConnectionRefresh(Long connectionId) {
        connectionRefreshScheduler.cancelConnectionRefresh(connectionId);
    }
}
