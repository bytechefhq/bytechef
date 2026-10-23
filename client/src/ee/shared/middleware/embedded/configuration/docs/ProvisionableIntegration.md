
# ProvisionableIntegration

An integration that can be provisioned to a connected user.

## Properties

Name | Type
------------ | -------------
`integration` | [IntegrationBasic](IntegrationBasic.md)
`componentVersion` | number
`integrationInstanceConfigurations` | [Array&lt;ProvisionableIntegrationInstanceConfiguration&gt;](ProvisionableIntegrationInstanceConfiguration.md)

## Example

```typescript
import type { ProvisionableIntegration } from ''

// TODO: Update the object below with actual values
const example = {
  "integration": null,
  "componentVersion": null,
  "integrationInstanceConfigurations": null,
} satisfies ProvisionableIntegration

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProvisionableIntegration
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


