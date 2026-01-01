
# Integration

A group of workflows that make one logical integration.

## Properties

Name | Type
------------ | -------------
`componentName` | string
`createdBy` | string
`createdDate` | Date
`description` | string
`icon` | string
`id` | number
`lastModifiedBy` | string
`lastModifiedDate` | Date
`lastPublishedDate` | Date
`lastStatus` | [IntegrationStatus](IntegrationStatus.md)
`lastIntegrationVersion` | number
`multipleInstances` | boolean
`name` | string
`category` | [Category](Category.md)
`integrationWorkflowIds` | Array&lt;number&gt;
`tags` | [Array&lt;Tag&gt;](Tag.md)
`version` | number

## Example

```typescript
import type { Integration } from ''

// TODO: Update the object below with actual values
const example = {
  "componentName": null,
  "createdBy": null,
  "createdDate": null,
  "description": null,
  "icon": null,
  "id": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "lastPublishedDate": null,
  "lastStatus": null,
  "lastIntegrationVersion": null,
  "multipleInstances": null,
  "name": null,
  "category": null,
  "integrationWorkflowIds": null,
  "tags": null,
  "version": null,
} satisfies Integration

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Integration
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


