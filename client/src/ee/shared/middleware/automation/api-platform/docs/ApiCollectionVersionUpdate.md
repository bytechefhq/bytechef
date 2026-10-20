
# ApiCollectionVersionUpdate

The outcome of moving an API collection to a newer project version.

## Properties

Name | Type
------------ | -------------
`apiCollection` | [ApiCollection](ApiCollection.md)
`disabledEndpointNames` | Array&lt;string&gt;
`missingConnections` | [Array&lt;ApiCollectionMissingConnection&gt;](ApiCollectionMissingConnection.md)
`missingInputs` | [Array&lt;ApiCollectionMissingInput&gt;](ApiCollectionMissingInput.md)
`removedEndpointNames` | Array&lt;string&gt;

## Example

```typescript
import type { ApiCollectionVersionUpdate } from ''

// TODO: Update the object below with actual values
const example = {
  "apiCollection": null,
  "disabledEndpointNames": null,
  "missingConnections": null,
  "missingInputs": null,
  "removedEndpointNames": null,
} satisfies ApiCollectionVersionUpdate

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApiCollectionVersionUpdate
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


