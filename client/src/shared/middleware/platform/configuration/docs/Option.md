
# Option

Defines valid property value.

## Properties

Name | Type
------------ | -------------
`description` | string
`label` | string
`value` | any

## Example

```typescript
import type { Option } from ''

// TODO: Update the object below with actual values
const example = {
  "description": null,
  "label": null,
  "value": null,
} satisfies Option

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Option
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


