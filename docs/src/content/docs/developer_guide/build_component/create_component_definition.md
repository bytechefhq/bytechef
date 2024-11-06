---
title: "Create Component Definition"
description: " "
---

In `server/libs/modules/components/yourconnector/src/main/java/com/bytechef/component/newcomponent` package, the `NewComponentComponentHandler` class defines the component. The `COMPONENT_DEFINITION` constant contains all the details about the component, including its name, title, description, icon, categories, connection, actions, and triggers.

```
private static final ComponentDefinition COMPONENT_DEFINITION = component("newComponent")
    .title("New Component")
    .description("New component description.")
    .icon("path:assets/new-component.svg")
    .categories(ComponentCategory.HELPERS)
    .connection(NewComponentConnection.CONNECTION_DEFINITION)
    .actions(NewComponentDummyAction.ACTION_DEFINITION)
    .triggers(NewComponentDummyTrigger.TRIGGER_DEFINITION);
```
