---
title: "Create Action"
description: "How to create an action for a component."
---

In `server/libs/modules/components/newcomponent/src/main/java/com/bytechef/component/newcomponent/action` package, the
`NewComponentDummyAction` class defines the connection. The `ACTION_DEFINITION` constant contains all the details about
the action, including its name, title, description, properties and others.

```
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



```
protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("some url"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body()
            .execute()
            .getBody(new TypeReference<>() {});
    }
```
