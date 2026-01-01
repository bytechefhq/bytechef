
# IntegrationInstance

Contains configurations and connections required for the execution of integration workflows for a connected user.

## Properties

Name | Type
------------ | -------------
`connectionId` | number
`connectedUserId` | number
`createdBy` | string
`createdDate` | Date
`enabled` | boolean
`environmentId` | number
`id` | number
`lastExecutionDate` | Date
`lastModifiedBy` | string
`lastModifiedDate` | Date
`integrationInstanceConfigurationId` | number
`integrationInstanceConfiguration` | [IntegrationInstanceConfigurationBasic](IntegrationInstanceConfigurationBasic.md)
`integrationInstanceWorkflows` | [Array&lt;IntegrationInstanceWorkflow&gt;](IntegrationInstanceWorkflow.md)
`version` | number

## Example

```typescript
import type { IntegrationInstance } from ''

// TODO: Update the object below with actual values
const example = {
  "connectionId": null,
  "connectedUserId": null,
  "createdBy": null,
  "createdDate": null,
  "enabled": null,
  "environmentId": null,
  "id": null,
  "lastExecutionDate": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "integrationInstanceConfigurationId": null,
  "integrationInstanceConfiguration": null,
  "integrationInstanceWorkflows": null,
  "version": null,
} satisfies IntegrationInstance

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IntegrationInstance
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


