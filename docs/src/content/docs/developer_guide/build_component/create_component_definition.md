---
title: "Create Component Definition"
description: "How to create a new component definition."
---

In `server/libs/modules/components/newcomponent/src/main/java/com/bytechef/component/newcomponent` package, the
`NewComponentComponentHandler` class defines the component. The `COMPONENT_DEFINITION` constant contains all the details
about the component, including its name, title, description, icon, categories, connection, actions, triggers and others.

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

### Icon

Find and download a user interface icon in .svg format for your component and place it in `server/libs/modules/components/newcomponent/src/main/resources/assets/newcomponent.svg`

For more information about any method in the `COMPONENT_DEFINITION`, refer to the [component documentation](/developer_guide/component_specification/component).
