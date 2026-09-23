
# Integration

A group of workflows that make one logical integration for a particular service represented by component.

## Properties

Name | Type
------------ | -------------
`componentName` | string
`description` | string
`icon` | string
`id` | number
`integrationInstances` | [Array&lt;IntegrationInstance&gt;](IntegrationInstance.md)
`integrationVersion` | number
`multipleInstances` | boolean
`name` | string
`connectionConfig` | [ConnectionConfig](ConnectionConfig.md)
`mcpWorkflows` | [Array&lt;IntegrationWorkflow&gt;](IntegrationWorkflow.md)
`mcpTools` | [Array&lt;McpTool&gt;](McpTool.md)
`workflows` | [Array&lt;IntegrationWorkflow&gt;](IntegrationWorkflow.md)

## Example

```typescript
import type { Integration } from ''

// TODO: Update the object below with actual values
const example = {
  "componentName": null,
  "description": null,
  "icon": null,
  "id": null,
  "integrationInstances": null,
  "integrationVersion": null,
  "multipleInstances": null,
  "name": null,
  "connectionConfig": null,
  "mcpWorkflows": null,
  "mcpTools": null,
  "workflows": null,
} satisfies Integration

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Integration
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


