package com.agui.core.event;

import com.agui.core.type.EventType;

/**
 * A concrete implementation of BaseEvent for custom user-defined events.
 * <p>
 * This event type is intended for application-specific events that don't
 * fit into the standard predefined event categories. It automatically
 * sets the event type to {@link EventType#CUSTOM}.
 * </p>
 * <p>
 * Custom events carry named payloads via {@link #name} and {@link #value}
 * fields, matching the AG-UI TypeScript {@code CustomEvent} schema
 * ({@code name: string, value: any}).
 * </p>
 *
 * @see BaseEvent
 * @see EventType#CUSTOM
 *
 * @author Pascal Wilbrink
 */
public class CustomEvent extends BaseEvent {

    private String name;

    private Object value;

    /**
     * Creates a new CustomEvent with type set to {@link EventType#CUSTOM}.
     * <p>
     * The timestamp is automatically set to the current time.
     * </p>
     */
    public CustomEvent() {
        super(EventType.CUSTOM);
    }

    /**
     * Creates a new CustomEvent with the given name and value.
     *
     * @param name  the event name, used by clients to identify the event kind
     * @param value the event payload; must be JSON-serialisable
     */
    public CustomEvent(String name, Object value) {
        super(EventType.CUSTOM);
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}