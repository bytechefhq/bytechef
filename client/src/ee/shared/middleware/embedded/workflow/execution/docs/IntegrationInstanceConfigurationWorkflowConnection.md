
# IntegrationInstanceConfigurationWorkflowConnection

The connection used in a particular task or trigger.

## Properties

Name | Type
------------ | -------------
`connectionId` | number
`workflowConnectionKey` | string
`workflowNodeName` | string

## Example

```typescript
import type { IntegrationInstanceConfigurationWorkflowConnection } from ''

// TODO: Update the object below with actual values
const example = {
  "connectionId": null,
  "workflowConnectionKey": null,
  "workflowNodeName": null,
} satisfies IntegrationInstanceConfigurationWorkflowConnection

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IntegrationInstanceConfigurationWorkflowConnection
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


