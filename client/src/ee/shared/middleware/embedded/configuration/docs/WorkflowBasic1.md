
# WorkflowBasic1

The blueprint that describe the execution of a job.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`description` | string
`id` | string
`label` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`version` | number

## Example

```typescript
import type { WorkflowBasic1 } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "description": null,
  "id": null,
  "label": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "version": null,
} satisfies WorkflowBasic1

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowBasic1
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


