---
title: "Adding a component manually"
description: "The structure of every Component is defined in the example module. For the reference of this guide, you can open that code."
---

#### The structure of every Component is defined in the [example](https://github.com/bytechefhq/bytechef/tree/master/server/libs/modules/components/example) module. For the reference of this guide, you can open that code.

### TABLE OF CONTENTS
- [Adding a component manually](#adding-a-component-manually)
    + [TABLE OF CONTENTS](#table-of-contents)
  * [Main](#main)
    + [Component Handler](#component-handler)
    + [Action](#actions)
      - [Modifiable Action Definition](#modifiable-action-definition)
      - [Perform](#perform)
    + [Trigger](#triggers)
      - [Modifiable Trigger Definition](#modifiable-trigger-definition)
    + [Connection](#connection)
    + [Constant](#constant)
    + [Resources](#resources)
  * [Test](#test)
    + [Action](#actions-1)
    + [Trigger](#triggers-1)
    + [Component Handler Test](#component-handler-test)
    + [Resources](#resources-1)

## Main

### Component Handler
The component is defined here. Everything about the component is connected here.

- `component` expects a string of what the system should know the component by, defined in [constant](#constant). It's best to name it the same as component's name.
- `title` and `description` are what the client displays about the component.
- `icon` takes the path to the .svg icon in the format as shown.
- `categories` defines what categories the component belongs to. A component can belong to multiple categories. You can see all categories in ComponentCategory class.
- `connection` expects one or more connections defined in [connection](#connection)
- `actions` expects one or more connections defined in [actions](#actions)

### Actions
This is where you define all the actions. Actions are simple functions that have properties as arguments and return an output.

#### Modifiable Action Definition
This defines the structure of the action.

- `title` and `description` are what the client displays about the action
- `properties` defines what kind of properties the component is going to have in the Properties tab. Because there is lots of different uses, the examples for various properties is located in other components. Constants are system names of those properties, defined in [constant](#constant).
- `ouput`
  + it can take an outputSchema which defines the properties of the output
  + it can also take a sample of an output as a String
  + it can have no arguments, which you use when the output of the action is undefined

#### Perform
This is where the logic of the action is stored.

- `inputParameters` is essentially getter for action properties
- `connectionParameters` is essentially a getter for connection properties
- `actionContext`
  - `file` works with files which it stores in a temporary memory. Usually outputs a FileEntry type
  - `data` works with data which it can store in a permanent memory
  - `logger` creates logs
  - `json` mainly reads JSONs
  - `http` sends http requests
  - `event` can publish an event

### Triggers
This is where you define all the triggers. Triggers are webhooks that have properties as arguments and return an output. There are different types of webhooks. Triggers are always the first to execute in workflows.

#### Modifiable Trigger Definition
This defines the structure of a trigger.

- `title` and `description` are what the client displays about the trigger
- `properties` defines what kind of properties the component is going to have in the Properties tab. Because there is lots of different uses, the examples for various properties is located in other components. Constants are system names of those properties, defined in [constant](#constant).
- `type` defines what TriggerDefinition.TriggerType the webhook is. Different types have different ways of registering.
- `ouput`
    + it can take an outputSchema which defines the properties of the output
    + it can also take a sample of an output as a String
    + it can have no arguments, which you use when the output of the action is undefined
- `webhookRequest` is the meat and potatoes of a trigger. The code that defines what happens when the event occurs; processing the data.
- `webhookEnable` is where the code for registering a webhook. Connecting to other systems usually using [connection](#connection) parameters and subscribing to them.
- `webhookDisable` is where the code for unsubscribing is
- `webhookValidate` is where the code for validation is. You don't need this one if you have `webhookEnable`

### Connection
This is where the connection is defined.

- `baseUri` defines the base uri of the connection
- `authorizations`

### Constant
Constant contains all string constants of all properties. Please respect the java convention. Strings should be named the same as property names, but in camel case.

### Resources
Inside the [assets](https://github.com/bytechefhq/bytechef/tree/master/server/libs/modules/components/example/src/main/resources/assets) folder should be a .svg icon. If the icon isn't shown correctly in the client, you could try finding another one or converting this one until it shows up correctly.

A [README](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/example/src/main/resources/README.md) file is optional. You could include it if you think the properties and description isn't self-explanatory.

## Test

### Actions
This is where you write Mockito tests for each Action's perform function. This is also where you can write integration tests. A better example of tests could be found in other components based on what functionality you need.

### Triggers


### Component Handler Test
This test autogenerates a json structure and compares it to the existing one in [resources](https://github.com/bytechefhq/bytechef/tree/master/server/libs/modules/components/example/src/test/resources/definition). If a json file doesn't exist, the autogenerated json will be put in that place.

### Resources
This where the json file will be located. If it isn't there yet, you can autogenerate one by running the [Component Handler Test](#component-handler-test)
