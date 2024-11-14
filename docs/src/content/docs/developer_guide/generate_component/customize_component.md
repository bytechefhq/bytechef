---
title: "Customize Component"
---

### Connector Icon

1. Find an Icon:
    - Search for a suitable user interface icon for your component in `.svg` format.

2. Save the Icon:
    - Place the icon in the following directory: `server/libs/modules/components/newcomponent/src/main/resources/assets/newcomponent.svg`.

3. Update Component Handler:
    - In `NewComponentComponentHandler.class`, override the `modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition)` method:
      ```java
      @Override
      public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
          return modifiableComponentDefinition.icon("path:assets/newcomponent.svg");
      }
      ```

### Connection

If your component requires custom authentication parameters, override the `modifyConnection(ModifiableConnectionDefinition modifiableConnectionDefinition)` method in `NewComponentComponentHandler.class`.

Refer to examples like [`ShopifyComponentHandler.class`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/shopify/src/main/java/com/bytechef/component/shopify/ShopifyComponentHandler.java#L72), [`DiscordComponentHandler.class`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/discord/src/main/java/com/bytechef/component/discord/DiscordComponentHandler.java#L92), or [`PipelinerComponentHandler.class`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/pipeliner/src/main/java/com/bytechef/component/pipeliner/PipelinerComponentHandler.java#L57) for guidance.

### Dynamic options

For parameters that require dynamic options, override the `modifyProperty(ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty)` method in `NewComponentComponentHandler.class`.

Check examples such as [`ShopifyComponentHandler.class`](https://github.com/bytechefhq/bytechef/blob/master/server/libs/modules/components/shopify/src/main/java/com/bytechef/component/shopify/ShopifyComponentHandler.java#L96) for implementation details.
