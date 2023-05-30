package com.bytechef.hermes.remote.client.service;

import com.bytechef.hermes.service.TriggerLifecycleService;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerLifecycleServiceClient implements TriggerLifecycleService {

    @Override
    public <T> Optional<T> fetchValue(long instanceId, String workflowExecutionId) {
        return Optional.empty();
    }

    @Override
    public void save(long instanceId, String workflowExecutionId, Object value) {

    }
}
