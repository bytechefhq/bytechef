
# ComponentProperty

A resolved component input property the SDK renders.

## Properties

Name | Type
------------ | -------------
`name` | string
`label` | string
`type` | [InputType](InputType.md)
`controlType` | string
`required` | boolean
`options` | [Array&lt;Option&gt;](Option.md)
`dynamicOptions` | boolean
`optionsLookupDependsOn` | Array&lt;string&gt;

## Example

```typescript
import type { ComponentProperty } from ''

// TODO: Update the object below with actual values
const example = {
  "name": null,
  "label": null,
  "type": null,
  "controlType": null,
  "required": null,
  "options": null,
  "dynamicOptions": null,
  "optionsLookupDependsOn": null,
} satisfies ComponentProperty

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ComponentProperty
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


