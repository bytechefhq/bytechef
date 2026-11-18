
# ErrorsInner


## Properties

Name | Type
------------ | -------------
`id` | string
`detail` | string
`problemType` | string
`title` | string
`code` | string
`status` | string
`meta` | [ErrorsInnerMeta](ErrorsInnerMeta.md)

## Example

```typescript
import type { ErrorsInner } from ''

// TODO: Update the object below with actual values
const example = {
  "id": 9366efb4-8fb1-4a28-bfb0-8d6f9cc6b5c5,
  "detail": null,
  "problemType": MISSING_REQUIRED_FIELD,
  "title": Property values were not valid
,
  "code": MISSING_REQUIRED_FIELD,
  "status": 400,
  "meta": null,
} satisfies ErrorsInner

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ErrorsInner
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


