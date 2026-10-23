
# ConnectedUserImportResult


## Properties

Name | Type
------------ | -------------
`createdCount` | number
`rejectedRows` | [Array&lt;ConnectedUserImportRejectedRow&gt;](ConnectedUserImportRejectedRow.md)
`skippedExternalIds` | Array&lt;string&gt;

## Example

```typescript
import type { ConnectedUserImportResult } from ''

// TODO: Update the object below with actual values
const example = {
  "createdCount": null,
  "rejectedRows": null,
  "skippedExternalIds": null,
} satisfies ConnectedUserImportResult

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConnectedUserImportResult
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


