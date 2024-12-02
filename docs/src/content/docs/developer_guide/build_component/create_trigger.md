---
title: "Create Trigger"
description: "How to create a trigger for a component."
---

In `server/libs/modules/components/newcomponent/src/main/java/com/bytechef/component/newcomponent/trigger` package, the
`NewComponentDummyTrigger` class defines the trigger. The `TRIGGER_DEFINITION` constant contains all the details about
the trigger, including its name, title, description, properties and others.

``` java
public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = action("dummy")
        .title("Dummy Trigger")
        .description("Trigger description.")
        .properties()
        .output(
            outputSchema(
                string()));
```

For more information about any method in the `TRIGGER_DEFINITION`, refer to the [trigger documentation](/developer_guide/component_specification/trigger).
