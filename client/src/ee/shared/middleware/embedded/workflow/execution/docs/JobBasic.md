
# JobBasic

Represents an execution of a workflow.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`endDate` | Date
`id` | string
`label` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`metadata` | { [key: string]: any; }
`priority` | number
`startDate` | Date
`status` | string
`workflowId` | string

## Example

```typescript
import type { JobBasic } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "endDate": null,
  "id": null,
  "label": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "metadata": null,
  "priority": null,
  "startDate": null,
  "status": null,
  "workflowId": null,
} satisfies JobBasic

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as JobBasic
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


