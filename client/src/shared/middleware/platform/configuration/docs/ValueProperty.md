
# ValueProperty

A base property for all value based properties.

## Properties

Name | Type
------------ | -------------
`controlType` | [ControlType](ControlType.md)
`label` | string
`placeholder` | string

## Example

```typescript
import type { ValueProperty } from ''

// TODO: Update the object below with actual values
const example = {
  "controlType": null,
  "label": null,
  "placeholder": null,
} satisfies ValueProperty

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ValueProperty
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


