package com.agui.json;

import com.agui.core.event.BaseEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.message.BaseMessage;
import com.agui.core.state.State;
import com.agui.json.mixins.EventMixin;
import com.agui.json.mixins.MessageMixin;
import com.agui.json.mixins.RunErrorEventMixin;
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
        // Renames RunErrorEvent.error -> "message" on the wire to match the TypeScript Zod schema in
        // @ag-ui/core. Without this, a RUN_ERROR event from the Java server fails the browser-side
        // ZodError validator (`path: ["message"]`) and the real agent failure is hidden behind a
        // meta-failure to parse the failure event.
        module.setMixInAnnotation(RunErrorEvent.class, RunErrorEventMixin.class);
        module.setMixInAnnotation(State.class, StateMixin.class);

        return module;
    }

}
