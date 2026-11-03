
# ConfigurationMoveRequest

The target configuration and the connected users to move to it.

## Properties

Name | Type
------------ | -------------
`connectedUserIds` | Array&lt;number&gt;
`targetId` | number

## Example

```typescript
import type { ConfigurationMoveRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "connectedUserIds": null,
  "targetId": null,
} satisfies ConfigurationMoveRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConfigurationMoveRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


