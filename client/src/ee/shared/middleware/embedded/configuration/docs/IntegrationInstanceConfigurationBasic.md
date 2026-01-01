
# IntegrationInstanceConfigurationBasic

Contains configurations and connections required for the execution of integration workflows.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`description` | string
`enabled` | boolean
`environmentId` | number
`id` | number
`integrationId` | number
`integrationVersion` | number
`lastModifiedBy` | string
`lastModifiedDate` | Date
`name` | string
`authorizationType` | [AuthorizationType](AuthorizationType.md)

## Example

```typescript
import type { IntegrationInstanceConfigurationBasic } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "description": null,
  "enabled": null,
  "environmentId": null,
  "id": null,
  "integrationId": null,
  "integrationVersion": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "name": null,
  "authorizationType": null,
} satisfies IntegrationInstanceConfigurationBasic

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IntegrationInstanceConfigurationBasic
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


