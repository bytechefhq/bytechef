
# McpIntegrationInstanceTool

Contains user configurations for the execution of a particular MCP tool.

## Properties

Name | Type
------------ | -------------
`enabled` | boolean
`mcpToolId` | number

## Example

```typescript
import type { McpIntegrationInstanceTool } from ''

// TODO: Update the object below with actual values
const example = {
  "enabled": null,
  "mcpToolId": null,
} satisfies McpIntegrationInstanceTool

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as McpIntegrationInstanceTool
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


