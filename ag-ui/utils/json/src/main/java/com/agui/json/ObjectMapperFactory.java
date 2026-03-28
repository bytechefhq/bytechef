package com.agui.json;

import com.agui.core.event.BaseEvent;
import com.agui.core.message.BaseMessage;
import com.agui.core.state.State;
import com.agui.json.mixins.EventMixin;
import com.agui.json.mixins.MessageMixin;
import com.agui.json.mixins.StateMixin;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

/**
 * Factory class for creating a Jackson module with ag-ui-specific mixins.
 *
 * @author Pascal Wilbrink
 */
public class ObjectMapperFactory {

    private ObjectMapperFactory() { }

    /**
     * Creates a Jackson module that registers ag-ui mixins for proper serialization
     * of messages, events, and state objects.
     *
     * @return a configured JacksonModule with ag-ui mixins
     */
    public static JacksonModule createModule() {
        SimpleModule module = new SimpleModule("AgUiModule");

        module.setMixInAnnotation(BaseMessage.class, MessageMixin.class);
        module.setMixInAnnotation(BaseEvent.class, EventMixin.class);
        module.setMixInAnnotation(State.class, StateMixin.class);

        return module;
    }

}
