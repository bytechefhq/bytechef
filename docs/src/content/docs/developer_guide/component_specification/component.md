---
title: "Component"
---

The component definition is used to specify the properties of a component. Below is an explanation of each method that can be used in the component definition:

- `component(String name)` - Builds new `ModifiableComponentDefinition` with the specified name. The name defines the component key (backend ID).
- [`actions(A... actionDefinitions)`](/developer_guide/component_specification/action) - Specifies the actions that the component can perform.
- `categories(ComponentCategory... category)` - Defines the category or categories that the component belongs to, used to group components together in the UI.
- [`connection(ModifiableConnectionDefinition connectionDefinition)`](/developer_guide/component_specification/connection)  - Sets the connection definition for the component.
- `connectionRequired(boolean connectionRequired)` - Indicates whether the component requires a connection to be configured before it can be used.
- `customAction(boolean customAction)` - Indicates if the component is REST-based.
- `description(String description)` - Provides a short description of the component.
- `icon(String icon)` - Specifies the path to the icon that will be displayed in the UI.
- `title(String title)` - Sets the name of the component in Chicago Style that will be displayed in the UI.
- `triggers(T... triggerDefinitions)` - Lists the triggers that the component can listen to.
- `version(int version)` - Specifies the version number of the component.
