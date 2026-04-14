package com.bytechef.ee.platform.component.remote.web.rest.facade;

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.facade.ConnectionDefinitionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Nikolina Spehar
 */
@RestController
@RequestMapping("/remote/connection-definition-facade")
public class RemoteConnectionDefinitionFacadeController {

    private final ConnectionDefinitionFacade connectionDefinitionFacade;

    public RemoteConnectionDefinitionFacadeController(ConnectionDefinitionFacade connectionDefinitionFacade) {
        this.connectionDefinitionFacade = connectionDefinitionFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/refresh-connection",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<ComponentConnection> executeConnectionRefresh(
        @Valid @RequestBody ConnectionRefreshRequest connectionRefreshRequest) {

        return ResponseEntity.ok(connectionDefinitionFacade.executeConnectionRefresh(
            connectionRefreshRequest.tenantId, connectionRefreshRequest.connectionId));
    }

    @SuppressFBWarnings("EI")
    public record ConnectionRefreshRequest(String tenantId, Long connectionId) {
    }
}
