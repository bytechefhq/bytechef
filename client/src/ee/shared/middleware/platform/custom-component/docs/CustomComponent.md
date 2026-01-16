
# CustomComponent

An custom component.

## Properties

Name | Type
------------ | -------------
`componentVersion` | number
`createdBy` | string
`createdDate` | Date
`description` | string
`enabled` | boolean
`icon` | string
`id` | number
`language` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`name` | string
`title` | string
`version` | number

## Example

```typescript
import type { CustomComponent } from ''

// TODO: Update the object below with actual values
const example = {
  "componentVersion": null,
  "createdBy": null,
  "createdDate": null,
  "description": null,
  "enabled": null,
  "icon": null,
  "id": null,
  "language": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "name": null,
  "title": null,
  "version": null,
} satisfies CustomComponent

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CustomComponent
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


