---
title: "Create Trigger"
description: "How to create a trigger for a component."
---

When creating a trigger for generated component, it must be added manually. Follow the [instructions in Build Component section](/developer-guide/build-component/create-trigger) to create `NewComponentDummyTrigger` class that defines the trigger.

Once this class is created, update Component Handler as follows:
In `NewComponentComponentHandler`, override the `getTriggers()` method:

```java

@Override
public List<ModifiableTriggerDefinition> getTriggers() {
    return List.of(NewComponentDummyTrigger.TRIGGER_DEFINITION);
}
```
