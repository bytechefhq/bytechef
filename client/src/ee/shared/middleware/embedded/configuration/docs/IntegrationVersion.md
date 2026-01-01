
# IntegrationVersion

The integration version.

## Properties

Name | Type
------------ | -------------
`description` | string
`publishedDate` | Date
`version` | number
`status` | [IntegrationStatus](IntegrationStatus.md)

## Example

```typescript
import type { IntegrationVersion } from ''

// TODO: Update the object below with actual values
const example = {
  "description": null,
  "publishedDate": null,
  "version": null,
  "status": null,
} satisfies IntegrationVersion

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IntegrationVersion
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


