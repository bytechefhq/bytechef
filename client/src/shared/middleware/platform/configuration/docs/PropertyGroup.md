
# PropertyGroup

A named group of component input properties rendered as one compound input.

## Properties

Name | Type
------------ | -------------
`name` | string
`label` | string
`properties` | [Array&lt;Property&gt;](Property.md)

## Example

```typescript
import type { PropertyGroup } from ''

// TODO: Update the object below with actual values
const example = {
  "name": null,
  "label": null,
  "properties": null,
} satisfies PropertyGroup

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PropertyGroup
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


