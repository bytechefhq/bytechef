
# Authorization

Contains information required for a connection\'s authorization.

## Properties

Name | Type
------------ | -------------
`description` | string
`name` | string
`properties` | [Array&lt;Property&gt;](Property.md)
`title` | string
`type` | [AuthorizationType](AuthorizationType.md)

## Example

```typescript
import type { Authorization } from ''

// TODO: Update the object below with actual values
const example = {
  "description": null,
  "name": null,
  "properties": null,
  "title": null,
  "type": null,
} satisfies Authorization

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Authorization
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


