
# ComponentActionDynamicPropertiesRequest


## Properties

Name | Type
------------ | -------------
`actionName` | string
`componentName` | string
`componentVersion` | number
`connectionKey` | string
`lookupDependsOnPaths` | Array&lt;string&gt;
`parameters` | { [key: string]: any; }

## Example

```typescript
import type { ComponentActionDynamicPropertiesRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "actionName": null,
  "componentName": null,
  "componentVersion": null,
  "connectionKey": null,
  "lookupDependsOnPaths": null,
  "parameters": null,
} satisfies ComponentActionDynamicPropertiesRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ComponentActionDynamicPropertiesRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


