
# ApiConnector

An API connector.

## Properties

Name | Type
------------ | -------------
`connectorVersion` | string
`createdBy` | string
`createdDate` | Date
`description` | string
`definition` | string
`enabled` | boolean
`endpoints` | [Array&lt;ApiConnectorEndpoint&gt;](ApiConnectorEndpoint.md)
`icon` | string
`id` | number
`lastModifiedBy` | string
`lastModifiedDate` | Date
`name` | string
`specification` | string
`tags` | [Array&lt;Tag&gt;](Tag.md)
`title` | string
`version` | number

## Example

```typescript
import type { ApiConnector } from ''

// TODO: Update the object below with actual values
const example = {
  "connectorVersion": null,
  "createdBy": null,
  "createdDate": null,
  "description": null,
  "definition": null,
  "enabled": null,
  "endpoints": null,
  "icon": null,
  "id": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "name": null,
  "specification": null,
  "tags": null,
  "title": null,
  "version": null,
} satisfies ApiConnector

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApiConnector
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


