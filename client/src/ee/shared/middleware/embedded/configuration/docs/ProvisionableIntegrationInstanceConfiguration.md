
# ProvisionableIntegrationInstanceConfiguration

An integration instance configuration the integration can be provisioned onto.

## Properties

Name | Type
------------ | -------------
`integrationInstanceConfiguration` | [IntegrationInstanceConfiguration](IntegrationInstanceConfiguration.md)
`selfServe` | boolean
`sharedConnectionParameterNames` | Array&lt;string&gt;
`oAuth2AuthorizationParameters` | [OAuth2AuthorizationParameters](OAuth2AuthorizationParameters.md)
`redirectUri` | string

## Example

```typescript
import type { ProvisionableIntegrationInstanceConfiguration } from ''

// TODO: Update the object below with actual values
const example = {
  "integrationInstanceConfiguration": null,
  "selfServe": null,
  "sharedConnectionParameterNames": null,
  "oAuth2AuthorizationParameters": null,
  "redirectUri": null,
} satisfies ProvisionableIntegrationInstanceConfiguration

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProvisionableIntegrationInstanceConfiguration
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


