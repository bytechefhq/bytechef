
# TaskExecution

Adds execution semantics to a task.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`endDate` | Date
`error` | [ExecutionError](ExecutionError.md)
`executionTime` | number
`icon` | string
`id` | string
`input` | { [key: string]: any; }
`jobId` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`maxRetries` | number
`output` | object
`parentId` | string
`priority` | number
`progress` | number
`retryAttempts` | number
`retryDelay` | string
`retryDelayFactor` | number
`startDate` | Date
`status` | string
`taskNumber` | number
`title` | string
`retryDelayMillis` | number
`workflowTask` | [WorkflowTask](WorkflowTask.md)
`type` | string

## Example

```typescript
import type { TaskExecution } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "endDate": null,
  "error": null,
  "executionTime": null,
  "icon": null,
  "id": null,
  "input": null,
  "jobId": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "maxRetries": null,
  "output": null,
  "parentId": null,
  "priority": null,
  "progress": null,
  "retryAttempts": null,
  "retryDelay": null,
  "retryDelayFactor": null,
  "startDate": null,
  "status": null,
  "taskNumber": null,
  "title": null,
  "retryDelayMillis": null,
  "workflowTask": null,
  "type": null,
} satisfies TaskExecution

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TaskExecution
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


