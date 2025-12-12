
# ComponentDefinition

A component contains a set of reusable code(actions) that accomplish specific tasks, triggers and connections if there is a need for a connection to an outside service.

## Properties

Name | Type
------------ | -------------
`actionClusterElementTypes` | { [key: string]: Array&lt;string&gt;; }
`actions` | [Array&lt;ActionDefinitionBasic&gt;](ActionDefinitionBasic.md)
`clusterElement` | boolean
`clusterElementClusterElementTypes` | { [key: string]: Array&lt;string&gt;; }
`clusterElements` | [Array&lt;ClusterElementDefinitionBasic&gt;](ClusterElementDefinitionBasic.md)
`clusterElementTypes` | [Array&lt;ClusterElementType&gt;](ClusterElementType.md)
`clusterRoot` | boolean
`componentCategories` | [Array&lt;ComponentCategory&gt;](ComponentCategory.md)
`connection` | [ConnectionDefinitionBasic](ConnectionDefinitionBasic.md)
`connectionRequired` | boolean
`description` | string
`icon` | string
`name` | string
`resources` | [Resources](Resources.md)
`tags` | Array&lt;string&gt;
`title` | string
`triggers` | [Array&lt;TriggerDefinitionBasic&gt;](TriggerDefinitionBasic.md)
`unifiedApiCategory` | [UnifiedApiCategory](UnifiedApiCategory.md)
`version` | number

## Example

```typescript
import type { ComponentDefinition } from ''

// TODO: Update the object below with actual values
const example = {
  "actionClusterElementTypes": null,
  "actions": null,
  "clusterElement": null,
  "clusterElementClusterElementTypes": null,
  "clusterElements": null,
  "clusterElementTypes": null,
  "clusterRoot": null,
  "componentCategories": null,
  "connection": null,
  "connectionRequired": null,
  "description": null,
  "icon": null,
  "name": null,
  "resources": null,
  "tags": null,
  "title": null,
  "triggers": null,
  "unifiedApiCategory": null,
  "version": null,
} satisfies ComponentDefinition

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ComponentDefinition
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


