
# ProvisionIntegrationInstanceRequest

The request to provision an integration instance.

## Properties

Name | Type
------------ | -------------
`integrationInstanceConfigurationId` | number
`connectionParameters` | { [key: string]: any; }
`workflows` | [Array&lt;ProvisionIntegrationInstanceWorkflow&gt;](ProvisionIntegrationInstanceWorkflow.md)

## Example

```typescript
import type { ProvisionIntegrationInstanceRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "integrationInstanceConfigurationId": null,
  "connectionParameters": null,
  "workflows": null,
} satisfies ProvisionIntegrationInstanceRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProvisionIntegrationInstanceRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


