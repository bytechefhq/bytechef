---
title: "Trigger"
---

The trigger definition is used to specify the properties of a trigger. Below is an explanation of each method that can be used in the trigger definition:

- `trigger(String name)` - Builds new `ModifiableTriggerDefinition` with the specified name. The name defines the trigger key (backend ID).
- `description(String description)` - Provides a short description of the trigger.
- `output` - Defines the output of the trigger.
- `properties(P... properties)` - Lists the properties that the trigger needs to perform its task. Properties will be shown in the Properties tab. For more information, refer to the [Property](/developer-guide/component-specification/property).
- `title(String title)` - Sets the name of the trigger that will be displayed in the UI.
- `type(TriggerType type)` - Sets the type of the trigger. Possible types are `DYNAMIC_WEBHOOK`, `HYBRID`, `LISTENER`, `POLLING` and `STATIC_WEBHOOK`.

## Trigger Type

- **DYNAMIC_WEBHOOK**: A trigger that listens for incoming HTTP requests at a dynamically generated URL.
- **HYBRID**: Combines features of both polling and webhook triggers. It can listen for events via webhooks and also poll for updates, providing flexibility in handling different event sources.
- **LISTENER**: A trigger that continuously listens for specific events or messages from a source, such as a message queue or event stream, and activates when those events occur.
- **POLLING**: Regularly checks a data source at specified intervals to detect changes or new data. This type is suitable for systems that do not support webhooks or real-time notifications.
- **STATIC_WEBHOOK**: A trigger that listens for incoming HTTP requests at a fixed URL. This type is ideal for scenarios where the endpoint URL does not change and can be predefined.
