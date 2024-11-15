---
title: "Customize Component"
---

### Connector Icon and Category

1. Find an Icon:
    - Search for a suitable user interface icon for your component in `.svg` format.

2. Save the Icon:
    - Place the icon in the following directory: `server/libs/modules/components/newcomponent/src/main/resources/assets/newcomponent.svg`.

3. Choose a Category:
    - Select a category for your component. Available categories can be found in [ComponentCategory](https://github.com/bytechefhq/bytechef/blob/master/sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentCategory.java).

4. Update Component Handler:
    - In `NewComponentComponentHandler`, override the `modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition)` method:
      ```java
      @Override
      public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
          return modifiableComponentDefinition
                .icon("path:assets/newcomponent.svg")
                .categories(ComponentCategory.HELPERS);
      }
      ```

### Connection

If your component requires custom authentication parameters, override the `modifyConnection(ModifiableConnectionDefinition modifiableConnectionDefinition)` method in `NewComponentComponentHandler`.

Refer to examples like [`ShopifyComponentHandler`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/shopify/src/main/java/com/bytechef/component/shopify/ShopifyComponentHandler.java#L72), [`DiscordComponentHandler`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/discord/src/main/java/com/bytechef/component/discord/DiscordComponentHandler.java#L92), or [`PipelinerComponentHandler`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/pipeliner/src/main/java/com/bytechef/component/pipeliner/PipelinerComponentHandler.java#L57) for guidance.

### Action

If some actions require properties not specified in the OpenAPI schema, override the `modifyActions(ModifiableActionDefinition... actionDefinitions)` method in `NewComponentComponentHandler`.

Refer to examples like [`DiscordComponentHandler`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/discord/src/main/java/com/bytechef/component/discord/DiscordComponentHandler.java#L66) or [`ClickupComponentHandler`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/clickup/src/main/java/com/bytechef/component/clickup/ClickupComponentHandler.java#L60).

### Dynamic options

For parameters that require dynamic options, override the `modifyProperty(ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty)` method in `NewComponentComponentHandler`.

Check examples such as [`ShopifyComponentHandler`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/shopify/src/main/java/com/bytechef/component/shopify/ShopifyComponentHandler.java#L96) for implementation details.
