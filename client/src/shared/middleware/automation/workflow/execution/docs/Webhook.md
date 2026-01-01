
# Webhook

Used to register to receive notifications for certain events.

## Properties

Name | Type
------------ | -------------
`type` | string
`url` | string
`retry` | [WebhookRetry](WebhookRetry.md)

## Example

```typescript
import type { Webhook } from ''

// TODO: Update the object below with actual values
const example = {
  "type": null,
  "url": null,
  "retry": null,
} satisfies Webhook

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Webhook
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


