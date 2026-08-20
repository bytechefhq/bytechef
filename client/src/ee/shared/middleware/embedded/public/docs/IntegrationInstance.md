
# IntegrationInstance

The integration instance represents a configured integration for a specific user, containing connection and status information

## Properties

Name | Type
------------ | -------------
`id` | number
`credentialStatus` | [CredentialStatus](CredentialStatus.md)
`enabled` | boolean
`mcpTools` | [Array&lt;McpIntegrationInstanceTool&gt;](McpIntegrationInstanceTool.md)
`mcpWorkflows` | [Array&lt;IntegrationInstanceWorkflow&gt;](IntegrationInstanceWorkflow.md)
`workflows` | [Array&lt;IntegrationInstanceWorkflow&gt;](IntegrationInstanceWorkflow.md)

## Example

```typescript
import type { IntegrationInstance } from ''

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "credentialStatus": null,
  "enabled": null,
  "mcpTools": null,
  "mcpWorkflows": null,
  "workflows": null,
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


