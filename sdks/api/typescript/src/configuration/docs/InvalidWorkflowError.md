
# InvalidWorkflowError

Returned when an automation cannot be enabled because the workflow that would run has validation errors.

## Properties

Name | Type
------------ | -------------
`invalidWorkflowErrorCount` | number

## Example

```typescript
import type { InvalidWorkflowError } from ''

// TODO: Update the object below with actual values
const example = {
  "invalidWorkflowErrorCount": null,
} satisfies InvalidWorkflowError

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as InvalidWorkflowError
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


