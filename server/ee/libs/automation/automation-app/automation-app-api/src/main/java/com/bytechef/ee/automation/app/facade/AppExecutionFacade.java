/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.dto.AppActionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppToolDTO;
import com.bytechef.ee.automation.app.proxy.ProxyRequest;
import com.bytechef.ee.automation.app.proxy.ProxyResponse;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The execution surface an app calls with its own secret key. Every method acts for the current {@code AppPrincipal}
 * and honors only the operations allowed on the app's bindings.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppExecutionFacade {

    Object executeAction(
        String bindingKey, String actionName, @Nullable Integer componentVersion, Map<String, Object> input);

    Object executeTool(String bindingKey, String toolName, Map<String, Object> input);

    /**
     * Returns the actions the calling app may execute across its bindings, with input schemas from the latest component
     * version. Skips bindings whose component cannot be resolved, and allowed action names missing from the latest
     * component version.
     */
    List<AppActionDTO> getActions();

    /**
     * Returns the connections bound to the current app.
     */
    List<AppBindingDTO> getBindings();

    List<AppToolDTO> getTools();

    /**
     * Sends a raw HTTP request through the connection bound under {@code bindingKey}, with the connection's credentials
     * applied. The binding's proxy switch and method allow-list, the path rules and the size limits are enforced by the
     * proxy executor, not by this facade.
     */
    ProxyResponse proxy(String bindingKey, ProxyRequest request);
}
