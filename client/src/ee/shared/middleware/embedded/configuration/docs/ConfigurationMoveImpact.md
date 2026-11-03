
# ConfigurationMoveImpact

What moving connected users to another configuration would change.

## Properties

Name | Type
------------ | -------------
`mcpServerCount` | number
`skipped` | [Array&lt;ConfigurationMoveSkipped&gt;](ConfigurationMoveSkipped.md)
`workflows` | [Array&lt;ConfigurationMoveImpactWorkflow&gt;](ConfigurationMoveImpactWorkflow.md)

## Example

```typescript
import type { ConfigurationMoveImpact } from ''

// TODO: Update the object below with actual values
const example = {
  "mcpServerCount": null,
  "skipped": null,
  "workflows": null,
} satisfies ConfigurationMoveImpact

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConfigurationMoveImpact
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


