
# WorkflowTestConfiguration

Contains configuration and connections required for the test execution of a particular workflow.

## Properties

Name | Type
------------ | -------------
`connections` | [Array&lt;WorkflowTestConfigurationConnection&gt;](WorkflowTestConfigurationConnection.md)
`createdBy` | string
`createdDate` | Date
`environmentId` | number
`inputs` | { [key: string]: string; }
`lastModifiedBy` | string
`lastModifiedDate` | Date
`workflowId` | string
`version` | number

## Example

```typescript
import type { WorkflowTestConfiguration } from ''

// TODO: Update the object below with actual values
const example = {
  "connections": null,
  "createdBy": null,
  "createdDate": null,
  "environmentId": null,
  "inputs": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "workflowId": null,
  "version": null,
} satisfies WorkflowTestConfiguration

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowTestConfiguration
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


