
# AgentChannelDefinition

A trigger/reply-action pair through which an AI Agent can be reached.

## Properties

Name | Type
------------ | -------------
`approvalChannelName` | string
`attachmentsPath` | string
`componentName` | string
`componentVersion` | number
`conversationIdPath` | string
`description` | string
`messagePath` | string
`name` | string
`replyActionName` | string
`replyAttachmentsProperty` | string
`replyChannelParameters` | { [key: string]: string; }
`replyConversationIdProperty` | string
`replyFixedParameters` | { [key: string]: any; }
`replyMessageProperty` | string
`title` | string
`triggerName` | string
`triggerParameters` | { [key: string]: any; }

## Example

```typescript
import type { AgentChannelDefinition } from ''

// TODO: Update the object below with actual values
const example = {
  "approvalChannelName": null,
  "attachmentsPath": null,
  "componentName": null,
  "componentVersion": null,
  "conversationIdPath": null,
  "description": null,
  "messagePath": null,
  "name": null,
  "replyActionName": null,
  "replyAttachmentsProperty": null,
  "replyChannelParameters": null,
  "replyConversationIdProperty": null,
  "replyFixedParameters": null,
  "replyMessageProperty": null,
  "title": null,
  "triggerName": null,
  "triggerParameters": null,
} satisfies AgentChannelDefinition

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AgentChannelDefinition
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


