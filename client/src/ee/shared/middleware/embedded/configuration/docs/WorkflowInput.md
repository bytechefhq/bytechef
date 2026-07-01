
# WorkflowInput


## Properties

Name | Type
------------ | -------------
`internalOnly` | boolean
`label` | string
`name` | string
`objectName` | string
`required` | boolean
`type` | string
`componentReference` | [ComponentInputReference](ComponentInputReference.md)

## Example

```typescript
import type { WorkflowInput } from ''

// TODO: Update the object below with actual values
const example = {
  "internalOnly": null,
  "label": null,
  "name": null,
  "objectName": null,
  "required": null,
  "type": null,
  "componentReference": null,
} satisfies WorkflowInput

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowInput
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


