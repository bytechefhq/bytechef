
# TriggerExecution

Adds execution semantics to a trigger.

## Properties

Name | Type
------------ | -------------
`batch` | boolean
`createdBy` | string
`createdDate` | Date
`endDate` | Date
`error` | [ExecutionError](ExecutionError.md)
`executionTime` | number
`icon` | string
`id` | string
`input` | { [key: string]: any; }
`lastModifiedBy` | string
`lastModifiedDate` | Date
`maxRetries` | number
`output` | object
`priority` | number
`retryAttempts` | number
`retryDelay` | string
`retryDelayFactor` | number
`retryDelayMillis` | number
`startDate` | Date
`status` | string
`workflowTrigger` | [WorkflowTrigger](WorkflowTrigger.md)
`title` | string
`type` | string

## Example

```typescript
import type { TriggerExecution } from ''

// TODO: Update the object below with actual values
const example = {
  "batch": null,
  "createdBy": null,
  "createdDate": null,
  "endDate": null,
  "error": null,
  "executionTime": null,
  "icon": null,
  "id": null,
  "input": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "maxRetries": null,
  "output": null,
  "priority": null,
  "retryAttempts": null,
  "retryDelay": null,
  "retryDelayFactor": null,
  "retryDelayMillis": null,
  "startDate": null,
  "status": null,
  "workflowTrigger": null,
  "title": null,
  "type": null,
} satisfies TriggerExecution

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TriggerExecution
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


