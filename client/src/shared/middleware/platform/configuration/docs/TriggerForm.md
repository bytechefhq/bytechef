
# TriggerForm


## Properties

Name | Type
------------ | -------------
`buttonLabel` | string
`customFormStyling` | string
`formDescription` | string
`formPath` | string
`formTitle` | string
`appendAttribution` | boolean
`ignoreBots` | boolean
`useWorkflowTimezone` | boolean
`inputs` | [Array&lt;TriggerFormInput&gt;](TriggerFormInput.md)

## Example

```typescript
import type { TriggerForm } from ''

// TODO: Update the object below with actual values
const example = {
  "buttonLabel": null,
  "customFormStyling": null,
  "formDescription": null,
  "formPath": null,
  "formTitle": null,
  "appendAttribution": null,
  "ignoreBots": null,
  "useWorkflowTimezone": null,
  "inputs": null,
} satisfies TriggerForm

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TriggerForm
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


