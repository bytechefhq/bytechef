
# Job

Represents an execution of a workflow.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`currentTask` | number
`endDate` | Date
`error` | [ExecutionError](ExecutionError.md)
`id` | string
`inputs` | { [key: string]: any; }
`label` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`metadata` | { [key: string]: any; }
`outputs` | { [key: string]: any; }
`parentTaskExecutionId` | number
`priority` | number
`startDate` | Date
`status` | string
`taskExecutions` | [Array&lt;TaskExecution&gt;](TaskExecution.md)
`webhooks` | [Array&lt;Webhook&gt;](Webhook.md)
`workflowId` | string

## Example

```typescript
import type { Job } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "currentTask": null,
  "endDate": null,
  "error": null,
  "id": null,
  "inputs": null,
  "label": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "metadata": null,
  "outputs": null,
  "parentTaskExecutionId": null,
  "priority": null,
  "startDate": null,
  "status": null,
  "taskExecutions": null,
  "webhooks": null,
  "workflowId": null,
} satisfies Job

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Job
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


