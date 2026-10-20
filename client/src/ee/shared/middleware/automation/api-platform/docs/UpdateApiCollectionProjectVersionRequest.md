
# UpdateApiCollectionProjectVersionRequest

The request object that names the project version to move an API collection to.

## Properties

Name | Type
------------ | -------------
`projectVersion` | number
`removeMissingEndpoints` | boolean

## Example

```typescript
import type { UpdateApiCollectionProjectVersionRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "projectVersion": null,
  "removeMissingEndpoints": null,
} satisfies UpdateApiCollectionProjectVersionRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateApiCollectionProjectVersionRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


