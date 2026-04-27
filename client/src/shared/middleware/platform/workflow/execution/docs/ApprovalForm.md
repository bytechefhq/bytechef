
# ApprovalForm


## Properties

Name | Type
------------ | -------------
`environmentId` | number
`formDescription` | string
`formTitle` | string
`inputs` | [Array&lt;ApprovalFormInput&gt;](ApprovalFormInput.md)

## Example

```typescript
import type { ApprovalForm } from ''

// TODO: Update the object below with actual values
const example = {
  "environmentId": null,
  "formDescription": null,
  "formTitle": null,
  "inputs": null,
} satisfies ApprovalForm

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApprovalForm
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


