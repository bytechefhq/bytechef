package com.bytechef.ee.automation.configuration.remote.client.facade;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.connection.domain.ConnectionEnvironment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteWorkspaceConnectionFacadeClient implements WorkspaceConnectionFacade {

    @Override
    public long create(long workspaceId, com.bytechef.platform.connection.dto.ConnectionDTO connectionDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<com.bytechef.platform.connection.dto.ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, ConnectionEnvironment connectionEnvironment,
        Long tagId) {

        throw new UnsupportedOperationException();
    }
}
