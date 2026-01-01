
# ComponentDefinitionBasic

A component contains a set of reusable code(actions) that accomplish specific tasks, triggers and connections if there is a need for a connection to an outside service.

## Properties

Name | Type
------------ | -------------
`actionsCount` | number
`clusterElementsCount` | { [key: string]: number; }
`componentCategories` | [Array&lt;ComponentCategory&gt;](ComponentCategory.md)
`description` | string
`icon` | string
`name` | string
`title` | string
`triggersCount` | number
`version` | number

## Example

```typescript
import type { ComponentDefinitionBasic } from ''

// TODO: Update the object below with actual values
const example = {
  "actionsCount": null,
  "clusterElementsCount": null,
  "componentCategories": null,
  "description": null,
  "icon": null,
  "name": null,
  "title": null,
  "triggersCount": null,
  "version": null,
} satisfies ComponentDefinitionBasic

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ComponentDefinitionBasic
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


