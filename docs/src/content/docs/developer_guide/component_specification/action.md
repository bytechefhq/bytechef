---
title: "Action"
---

The action definition is used to specify the properties of an action. Below is an explanation of each method that can be used in the action definition:

- `action(String name)` - Builds new `ModifiableActionDefinition` with the specified name. The name defines the action key (backend ID).
- `description(String description)` - Provides a short description of the action.
- `perform(SingleConnectionPerformFunction perform)` - The function that store logic for the action.
  Perform method has the following arguments:
    - `Parameters inputParameters` - Acts as a getter for action properties.
    - `Parameters connectionParameters` - Acts as a getter for connection properties.
    - `ActionContext actionContext`
        - `file` - Works with files stored in temporary memory, usually outputs a FileEntry type.
        - `data`- Works with data that can be stored in permanent memory.
        - `logger` - Creates logs.
        - `json` - Mainly reads JSONs.
        - `http` - Sends HTTP requests.
        - `event` - Can publish an event.
- `output` - Defines the output of the action.
- `properties(P... properties)` - Lists the properties that the action needs to perform its task. Properties will be shown in the Properties tab. For more information, refer to the [Property](/developer_guide/component_specification/property).
- `title(String title)` - Sets the name of the action in Chicago Style that will be displayed in the UI.
