
# WorkflowTask

Represents a definition of a workflow task.

## Properties

Name | Type
------------ | -------------
`clusterRoot` | boolean
`clusterElements` | { [key: string]: any; }
`connections` | [Array&lt;ComponentConnection&gt;](ComponentConnection.md)
`description` | string
`finalize` | [Array&lt;WorkflowTask&gt;](WorkflowTask.md)
`label` | string
`metadata` | { [key: string]: any; }
`name` | string
`node` | string
`parameters` | { [key: string]: any; }
`post` | [Array&lt;WorkflowTask&gt;](WorkflowTask.md)
`pre` | [Array&lt;WorkflowTask&gt;](WorkflowTask.md)
`timeout` | string
`type` | string

## Example

```typescript
import type { WorkflowTask } from ''

// TODO: Update the object below with actual values
const example = {
  "clusterRoot": null,
  "clusterElements": null,
  "connections": null,
  "description": null,
  "finalize": null,
  "label": null,
  "metadata": null,
  "name": null,
  "node": null,
  "parameters": null,
  "post": null,
  "pre": null,
  "timeout": null,
  "type": null,
} satisfies WorkflowTask

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowTask
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


