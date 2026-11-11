
# AutomationProjectWorkflowTemplate

An automation workflow template within an automation project.

## Properties

Name | Type
------------ | -------------
`id` | string
`label` | string
`description` | string
`components` | [Array&lt;AutomationProjectComponent&gt;](AutomationProjectComponent.md)
`inputs` | [Array&lt;AutomationProjectWorkflowInput&gt;](AutomationProjectWorkflowInput.md)

## Example

```typescript
import type { AutomationProjectWorkflowTemplate } from ''

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "label": null,
  "description": null,
  "components": null,
  "inputs": null,
} satisfies AutomationProjectWorkflowTemplate

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AutomationProjectWorkflowTemplate
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


