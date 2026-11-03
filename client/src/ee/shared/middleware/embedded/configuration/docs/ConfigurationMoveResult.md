
# ConfigurationMoveResult

The outcome of moving connected users to another configuration.

## Properties

Name | Type
------------ | -------------
`movedConnectedUserIds` | Array&lt;number&gt;
`needsAttentionConnectedUserIds` | Array&lt;number&gt;
`skipped` | [Array&lt;ConfigurationMoveSkipped&gt;](ConfigurationMoveSkipped.md)

## Example

```typescript
import type { ConfigurationMoveResult } from ''

// TODO: Update the object below with actual values
const example = {
  "movedConnectedUserIds": null,
  "needsAttentionConnectedUserIds": null,
  "skipped": null,
} satisfies ConfigurationMoveResult

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConfigurationMoveResult
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


