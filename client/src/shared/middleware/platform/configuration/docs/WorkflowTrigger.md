
# WorkflowTrigger

Represents a definition of a workflow trigger.

## Properties

Name | Type
------------ | -------------
`connections` | [Array&lt;ComponentConnection&gt;](ComponentConnection.md)
`description` | string
`label` | string
`metadata` | { [key: string]: any; }
`name` | string
`parameters` | { [key: string]: any; }
`timeout` | string
`type` | string

## Example

```typescript
import type { WorkflowTrigger } from ''

// TODO: Update the object below with actual values
const example = {
  "connections": null,
  "description": null,
  "label": null,
  "metadata": null,
  "name": null,
  "parameters": null,
  "timeout": null,
  "type": null,
} satisfies WorkflowTrigger

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowTrigger
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


