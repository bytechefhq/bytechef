package com.bytechef.hermes.remote.client.service;

import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.service.TriggerExecutionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerExecutionServiceClient implements TriggerExecutionService {

    @Override
    public TriggerExecution create(TriggerExecution triggerExecution) {
        return null;
    }

    @Override
    public TriggerExecution getTriggerExecution(long id) {
        return null;
    }

    @Override
    public TriggerExecution update(TriggerExecution triggerExecution) {
        return null;
    }
}
