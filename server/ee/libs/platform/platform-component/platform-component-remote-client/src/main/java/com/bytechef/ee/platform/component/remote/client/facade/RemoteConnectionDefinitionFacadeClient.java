package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.ee.platform.component.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.facade.ConnectionDefinitionFacade;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionDefinitionFacadeClient implements ConnectionDefinitionFacade {

    @Override
    public ComponentConnection executeConnectionRefresh(String tenantId, Long connectionId) {
        throw new UnsupportedOperationException();
    }
}
