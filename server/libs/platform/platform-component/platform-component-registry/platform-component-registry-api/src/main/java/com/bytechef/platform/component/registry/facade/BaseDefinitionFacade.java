package com.bytechef.platform.component.registry.facade;

import com.bytechef.component.exception.ProviderException;
import org.springframework.lang.NonNull;

public interface BaseDefinitionFacade {
    ProviderException executeProcessErrorResponse(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, int statusCode,
        Object body);
}
