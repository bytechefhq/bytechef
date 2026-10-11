
# WorkflowTemplateAlreadyCopiedError

Returned when the connected user has already copied an automation workflow template.

## Properties

Name | Type
------------ | -------------
`copiedWorkflowUuid` | string

## Example

```typescript
import type { WorkflowTemplateAlreadyCopiedError } from ''

// TODO: Update the object below with actual values
const example = {
  "copiedWorkflowUuid": null,
} satisfies WorkflowTemplateAlreadyCopiedError

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowTemplateAlreadyCopiedError
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


