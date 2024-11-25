---
title: "Create Action"
description: "How to create an action for a component."
---

In `server/libs/modules/components/newcomponent/src/main/java/com/bytechef/component/newcomponent/action` package, the
`NewComponentDummyAction` class defines the connection. The `ACTION_DEFINITION` constant contains all the details about
the action, including its name, title, description, properties and others.

``` java
public static final ModifiableActionDefinition ACTION_DEFINITION = action("dummy")
        .title("Dummy Action")
        .description("Action description.")
        .properties(
            string("name")
                .label("label")
                .description("Property description.")
                .minLength(1)
                .maxLength(255)
                .required(true))
        .output(
            outputSchema(
                string()))
        .perform(NewComponentDummyAction::perform);
```

The `perform` method contains the logic for the action. Here is the simplest example of the `perform` method that returns the value of the `name` property.

``` java
protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return inputParemeters.getRequiredString("name");
    }
```

For more information about any method in the `ACTION_DEFINITION`, refer to the [action documentation](/developer_guide/component_specification/action).
