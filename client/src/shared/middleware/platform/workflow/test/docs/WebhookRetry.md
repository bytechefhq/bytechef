
# WebhookRetry


## Properties

Name | Type
------------ | -------------
`initialInterval` | number
`maxInterval` | number
`maxAttempts` | number
`multiplier` | number

## Example

```typescript
import type { WebhookRetry } from ''

// TODO: Update the object below with actual values
const example = {
  "initialInterval": null,
  "maxInterval": null,
  "maxAttempts": null,
  "multiplier": null,
} satisfies WebhookRetry

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WebhookRetry
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


