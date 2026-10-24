
# ClusterElementCallRecord

One call a task made to one of its cluster elements, such as an AI agent\'s tool or sub-agent.

## Properties

Name | Type
------------ | -------------
`componentName` | string
`endDate` | Date
`error` | string
`id` | string
`input` | string
`output` | string
`parentId` | string
`startDate` | Date
`status` | string
`toolName` | string
`workflowNodeName` | string

## Example

```typescript
import type { ClusterElementCallRecord } from ''

// TODO: Update the object below with actual values
const example = {
  "componentName": null,
  "endDate": null,
  "error": null,
  "id": null,
  "input": null,
  "output": null,
  "parentId": null,
  "startDate": null,
  "status": null,
  "toolName": null,
  "workflowNodeName": null,
} satisfies ClusterElementCallRecord

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ClusterElementCallRecord
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


