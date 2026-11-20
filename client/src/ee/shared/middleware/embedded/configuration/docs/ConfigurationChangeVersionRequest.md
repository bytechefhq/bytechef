
# ConfigurationChangeVersionRequest

Which connected user moves to which version, and optionally to which configuration.

## Properties

Name | Type
------------ | -------------
`connectedUserId` | number
`targetId` | number
`version` | number

## Example

```typescript
import type { ConfigurationChangeVersionRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "connectedUserId": null,
  "targetId": null,
  "version": null,
} satisfies ConfigurationChangeVersionRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConfigurationChangeVersionRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


