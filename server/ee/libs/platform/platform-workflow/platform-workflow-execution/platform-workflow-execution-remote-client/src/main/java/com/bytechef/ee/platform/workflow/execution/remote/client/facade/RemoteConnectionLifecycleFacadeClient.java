package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;

import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class RemoteConnectionLifecycleFacadeClient implements ConnectionLifecycleFacade {
    @Override
    public void scheduleConnectionRefresh(Long connectionId, Map<String, ?> parameters, AuthorizationType authorizationType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteScheduledConnectionRefresh(Long connectionId, AuthorizationType authorizationType) {
        throw new UnsupportedOperationException();
    }
}
