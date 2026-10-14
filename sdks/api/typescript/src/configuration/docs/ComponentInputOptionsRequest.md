
# ComponentInputOptionsRequest


## Properties

Name | Type
------------ | -------------
`componentName` | string
`componentVersion` | number
`groupName` | string
`propertyName` | string
`lookupDependsOnValues` | { [key: string]: any; }
`searchText` | string

## Example

```typescript
import type { ComponentInputOptionsRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "componentName": null,
  "componentVersion": null,
  "groupName": null,
  "propertyName": null,
  "lookupDependsOnValues": null,
  "searchText": null,
} satisfies ComponentInputOptionsRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ComponentInputOptionsRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


