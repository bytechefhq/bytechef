---
title: "Pipedrive"
description: "The first CRM designed by salespeople, for salespeople. Do more to grow your business."
---

The first CRM designed by salespeople, for salespeople. Do more to grow your business.


Categories: crm


Type: pipedrive/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Get Deals
Name: getDeals

Returns all deals.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| user_id | User ID | INTEGER | SELECT | Deals matching the given user will be returned. However, `filter_id` and `owned_by_you` takes precedence over `user_id` when supplied. | false |
| filter_id | Filter ID | INTEGER | SELECT | ID of the filter to use. | false |
| stage_id | Stage ID | INTEGER | SELECT | Deals within the given stage will be returned. | false |
| status | Status | STRING <details> <summary> Options </summary> open, won, lost, deleted, all_not_deleted </details> | SELECT |  | false |
| sort | Sort | STRING | TEXT | The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys). | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {[{INTEGER\(id), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}]\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Deals",
  "name" : "getDeals",
  "parameters" : {
    "user_id" : 1,
    "filter_id" : 1,
    "stage_id" : 1,
    "status" : "",
    "sort" : ""
  },
  "type" : "pipedrive/v1/getDeals"
}
```


### Add Deal
Name: addDeal

Adds a new deal.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Deal | OBJECT <details> <summary> Properties </summary> {STRING\(title), STRING\(value), STRING\(currency), INTEGER\(user_id), INTEGER\(person_id), INTEGER\(org_id), INTEGER\(pipeline_id), INTEGER\(stage_id), STRING\(status), DATE\(expected_close_date), NUMBER\(probability), STRING\(lost_reason)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Add Deal",
  "name" : "addDeal",
  "parameters" : {
    "__item" : {
      "title" : "",
      "value" : "",
      "currency" : "",
      "user_id" : 1,
      "person_id" : 1,
      "org_id" : 1,
      "pipeline_id" : 1,
      "stage_id" : 1,
      "status" : "",
      "expected_close_date" : "2021-01-01",
      "probability" : 0.0,
      "lost_reason" : ""
    }
  },
  "type" : "pipedrive/v1/addDeal"
}
```


### Search Deals
Name: searchDeals

Searches all deals by title, notes and/or custom fields.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| term | Term | STRING | TEXT | The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded. | true |
| fields | Fields | STRING <details> <summary> Options </summary> custom_fields, notes, title </details> | SELECT | A comma-separated string array. The fields to perform the search from. Defaults to all of them. | false |
| exact_match | Exact Match | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive. | false |
| person_id | Person ID | INTEGER | SELECT | Will filter deals by the provided person. | false |
| organization_id | Organization ID | INTEGER | SELECT | Will filter deals by the provided organization. | false |
| status | Status | STRING <details> <summary> Options </summary> open, won, lost </details> | SELECT | Will filter deals by the provided specific status. | false |
| include_fields | Include Fields | STRING <details> <summary> Options </summary> deal.cc_email </details> | SELECT | Supports including optional fields in the results which are not provided by default. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{[{STRING\(id), STRING\(type), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}]\(items)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Search Deals",
  "name" : "searchDeals",
  "parameters" : {
    "term" : "",
    "fields" : "",
    "exact_match" : false,
    "person_id" : 1,
    "organization_id" : 1,
    "status" : "",
    "include_fields" : ""
  },
  "type" : "pipedrive/v1/searchDeals"
}
```


### Delete Deal
Name: deleteDeal

Marks a deal as deleted. After 30 days, the deal will be permanently deleted.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Deal ID | INTEGER | SELECT | Id of the deal to delete. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Delete Deal",
  "name" : "deleteDeal",
  "parameters" : {
    "id" : 1
  },
  "type" : "pipedrive/v1/deleteDeal"
}
```


### Get Details of Deal
Name: getDealDetails

Returns the details of a specific deal.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Deal | INTEGER | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Details of Deal",
  "name" : "getDealDetails",
  "parameters" : {
    "id" : 1
  },
  "type" : "pipedrive/v1/getDealDetails"
}
```


### Get Leads
Name: getLeads

Returns multiple leads. Leads are sorted by the time they were created, from oldest to newest.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| archived_status | Archived Status | STRING <details> <summary> Options </summary> archived, not_archived, all </details> | SELECT | Filtering based on the archived status of a lead. | false |
| owner_id | Owner iD | INTEGER | SELECT | Leads matching the given user will be returned. However, `filter_id` takes precedence over `owner_id` when supplied. | false |
| person_id | Person ID | INTEGER | SELECT | If supplied, only leads matching the given person will be returned. However, `filter_id` takes precedence over `person_id` when supplied. | false |
| organization_id | Organization ID | INTEGER | SELECT | If supplied, only leads matching the given organization will be returned. However, `filter_id` takes precedence over `organization_id` when supplied. | false |
| filter_id | Filter ID | INTEGER | SELECT | Filter to use | false |
| sort | Sort | STRING <details> <summary> Options </summary> id, title, owner_id, creator_id, was_seen, expected_close_date, next_activity_id, add_time, update_time </details> | SELECT | The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys). | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {[{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}]\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Leads",
  "name" : "getLeads",
  "parameters" : {
    "archived_status" : "",
    "owner_id" : 1,
    "person_id" : 1,
    "organization_id" : 1,
    "filter_id" : 1,
    "sort" : ""
  },
  "type" : "pipedrive/v1/getLeads"
}
```


### Add Lead
Name: addLead

Creates a lead. A lead always has to be linked to a person or an organization or both.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Lead | OBJECT <details> <summary> Properties </summary> {STRING\(title), INTEGER\(owner_id), [STRING]\(label_ids), INTEGER\(person_id), INTEGER\(organization_id), {NUMBER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Add Lead",
  "name" : "addLead",
  "parameters" : {
    "__item" : {
      "title" : "",
      "owner_id" : 1,
      "label_ids" : [ "" ],
      "person_id" : 1,
      "organization_id" : 1,
      "value" : {
        "amount" : 0.0,
        "currency" : ""
      },
      "expected_close_date" : "2021-01-01"
    }
  },
  "type" : "pipedrive/v1/addLead"
}
```


### Delete Lead
Name: deleteLead

Deletes a specific lead.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Lead ID | STRING | SELECT | The ID of the lead | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(id)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Delete Lead",
  "name" : "deleteLead",
  "parameters" : {
    "id" : ""
  },
  "type" : "pipedrive/v1/deleteLead"
}
```


### Get Lead Details
Name: getLeadDetails

Returns details of a specific lead. 

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Lead ID | STRING | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Lead Details",
  "name" : "getLeadDetails",
  "parameters" : {
    "id" : ""
  },
  "type" : "pipedrive/v1/getLeadDetails"
}
```


### Search Leads
Name: searchLeads

Searches all leads by title, notes and/or custom fields.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| term | Term | STRING | TEXT | The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded. | true |
| fields | Fields | STRING <details> <summary> Options </summary> custom_fields, notes, title </details> | SELECT | A comma-separated string array. The fields to perform the search from. Defaults to all of them. | false |
| exact_match | Exact Match | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive. | false |
| person_id | Person ID | INTEGER | SELECT | Will filter leads by the provided person ID. | false |
| organization_id | Organization ID | INTEGER | SELECT | Will filter leads by the provided organization ID. | false |
| include_fields | Include Fields | STRING <details> <summary> Options </summary> lead.was_seen </details> | SELECT | Supports including optional fields in the results which are not provided by default. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{[{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}]\(items)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Search Leads",
  "name" : "searchLeads",
  "parameters" : {
    "term" : "",
    "fields" : "",
    "exact_match" : false,
    "person_id" : 1,
    "organization_id" : 1,
    "include_fields" : ""
  },
  "type" : "pipedrive/v1/searchLeads"
}
```


### Get All Organizations
Name: getOrganizations

Returns all organizations.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| user_id | User ID | INTEGER | SELECT | Organizations owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied. | false |
| filter_id | Filter ID | INTEGER | SELECT | Filter to use | false |
| first_char | First Characters | STRING | TEXT | Organizations whose name starts with the specified letter will be returned (case insensitive) | false |
| sort | Sort | STRING | TEXT | The field names and sorting mode separated by a comma (`field_name_1ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys). | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}]\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get All Organizations",
  "name" : "getOrganizations",
  "parameters" : {
    "user_id" : 1,
    "filter_id" : 1,
    "first_char" : "",
    "sort" : ""
  },
  "type" : "pipedrive/v1/getOrganizations"
}
```


### Add Organization
Name: addOrganization

Adds a new organization.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Organization | OBJECT <details> <summary> Properties </summary> {STRING\(name), INTEGER\(owner_id)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Add Organization",
  "name" : "addOrganization",
  "parameters" : {
    "__item" : {
      "name" : "",
      "owner_id" : 1
    }
  },
  "type" : "pipedrive/v1/addOrganization"
}
```


### Search Organizations
Name: searchOrganization

Searches all organizations by name, address, notes and/or custom fields. This endpoint is a wrapper of <a href="https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem">/v1/itemSearch</a> with a narrower OAuth scope.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| term | Term | STRING | TEXT | The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded. | true |
| fields | Fields | STRING <details> <summary> Options </summary> address, custom_fields, notes, name </details> | SELECT | A comma-separated string array. The fields to perform the search from. Defaults to all of them. | false |
| exact_match | Exact Match | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}]\(items)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Search Organizations",
  "name" : "searchOrganization",
  "parameters" : {
    "term" : "",
    "fields" : "",
    "exact_match" : false
  },
  "type" : "pipedrive/v1/searchOrganization"
}
```


### Delete Organization
Name: deleteOrganization

Marks an organization as deleted. After 30 days, the organization will be permanently deleted.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Organization | INTEGER | SELECT | Organization to delete | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Delete Organization",
  "name" : "deleteOrganization",
  "parameters" : {
    "id" : 1
  },
  "type" : "pipedrive/v1/deleteOrganization"
}
```


### Get Details of Organization
Name: getOrganizationDetails

Returns the details of an organization.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Organizaton | INTEGER | SELECT | Organization to get details. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Details of Organization",
  "name" : "getOrganizationDetails",
  "parameters" : {
    "id" : 1
  },
  "type" : "pipedrive/v1/getOrganizationDetails"
}
```


### Get Persons
Name: getPersons

Returns all persons.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| user_id | User ID | INTEGER | SELECT | Persons owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied. | false |
| filter_id | Filter ID | INTEGER | SELECT | Filter to use. | false |
| first_char | First Characters | STRING | TEXT | Persons whose name starts with the specified letter will be returned (case insensitive) | false |
| sort | Sort | STRING | TEXT | The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys). | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}]\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Persons",
  "name" : "getPersons",
  "parameters" : {
    "user_id" : 1,
    "filter_id" : 1,
    "first_char" : "",
    "sort" : ""
  },
  "type" : "pipedrive/v1/getPersons"
}
```


### Add Person
Name: addPerson

Adds a new person.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Person | OBJECT <details> <summary> Properties </summary> {STRING\(name), INTEGER\(owner_id), INTEGER\(org_id), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Add Person",
  "name" : "addPerson",
  "parameters" : {
    "__item" : {
      "name" : "",
      "owner_id" : 1,
      "org_id" : 1,
      "email" : [ {
        "value" : "",
        "primary" : false,
        "label" : ""
      } ],
      "phone" : [ {
        "value" : "",
        "primary" : false,
        "label" : ""
      } ]
    }
  },
  "type" : "pipedrive/v1/addPerson"
}
```


### Search Persons
Name: searchPersons

Searches all persons by name, email, phone, notes and/or custom fields.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| term | Term | STRING | TEXT | The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded. | true |
| fields | Fields | STRING <details> <summary> Options </summary> custom_fields, email, notes, phone, name </details> | SELECT | A comma-separated string array. The fields to perform the search from. Defaults to all of them. | false |
| exact_match | Exact Match | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive. | false |
| organization_id | Organization ID | INTEGER | SELECT | Will filter persons by the provided organization. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}]\(items)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Search Persons",
  "name" : "searchPersons",
  "parameters" : {
    "term" : "",
    "fields" : "",
    "exact_match" : false,
    "organization_id" : 1
  },
  "type" : "pipedrive/v1/searchPersons"
}
```


### Delete Person
Name: deletePerson

Marks a person as deleted. After 30 days, the person will be permanently deleted.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Person | INTEGER | SELECT | Person to delete | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Delete Person",
  "name" : "deletePerson",
  "parameters" : {
    "id" : 1
  },
  "type" : "pipedrive/v1/deletePerson"
}
```


### Get Details of Person
Name: getPersonDetails

Returns the details of a person. This also returns some additional fields which are not present when asking for all persons.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| id | Person | INTEGER | SELECT | Person to get details | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| body | OBJECT <details> <summary> Properties </summary> {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}\(data)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Get Details of Person",
  "name" : "getPersonDetails",
  "parameters" : {
    "id" : 1
  },
  "type" : "pipedrive/v1/getPersonDetails"
}
```




## Triggers


### New Activity
Name: newActivity

Trigger off whenever a new activity is added.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| type_name | STRING | TEXT |
| public_description | STRING | TEXT |
| subject | STRING | TEXT |
| type | STRING | TEXT |
| id | INTEGER | INTEGER |
| owner_name | STRING | TEXT |
| user_id | INTEGER | INTEGER |
| company_id | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "New Activity",
  "name" : "newActivity",
  "type" : "pipedrive/v1/newActivity"
}
```


### New Deal
Name: newDeal

Trigger off whenever a new deal is added.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| email_messages_count | INTEGER | INTEGER |
| cc_email | STRING | TEXT |
| id | INTEGER | INTEGER |
| person_id | INTEGER | INTEGER |
| owner_name | STRING | TEXT |
| status | STRING | TEXT |
| title | STRING | TEXT |
| currency | STRING | TEXT |
| value | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "New Deal",
  "name" : "newDeal",
  "type" : "pipedrive/v1/newDeal"
}
```


### New Organization
Name: newOrganization

Trigger off whenever a new organization is added.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| email_messages_count | INTEGER | INTEGER |
| cc_email | STRING | TEXT |
| owner_id | INTEGER | INTEGER |
| id | INTEGER | INTEGER |
| owner_name | STRING | TEXT |
| name | STRING | TEXT |
| company_id | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "New Organization",
  "name" : "newOrganization",
  "type" : "pipedrive/v1/newOrganization"
}
```


### New Person
Name: newPerson

Trigger off whenever a new person is added.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| email_messages_count | INTEGER | INTEGER |
| cc_email | STRING | TEXT |
| owner_id | INTEGER | INTEGER |
| id | INTEGER | INTEGER |
| owner_name | STRING | TEXT |
| phone | ARRAY <details> <summary> Items </summary> [{STRING\(value), BOOLEAN\(primary)}] </details> | ARRAY_BUILDER |
| name | STRING | TEXT |
| email | ARRAY <details> <summary> Items </summary> [{STRING\(value), BOOLEAN\(primary)}] </details> | ARRAY_BUILDER |
| company_id | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "New Person",
  "name" : "newPerson",
  "type" : "pipedrive/v1/newPerson"
}
```


### Updated Deal
Name: updatedDeal

Trigger off whenever an existing deal is updated.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| email_messages_count | INTEGER | INTEGER |
| cc_email | STRING | TEXT |
| id | INTEGER | INTEGER |
| person_id | INTEGER | INTEGER |
| owner_name | STRING | TEXT |
| status | STRING | TEXT |
| title | STRING | TEXT |
| currency | STRING | TEXT |
| value | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "Updated Deal",
  "name" : "updatedDeal",
  "type" : "pipedrive/v1/updatedDeal"
}
```


### Updated Organization
Name: updatedOrganization

Trigger off whenever an existing organization is updated.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| email_messages_count | INTEGER | INTEGER |
| cc_email | STRING | TEXT |
| owner_id | INTEGER | INTEGER |
| id | INTEGER | INTEGER |
| owner_name | STRING | TEXT |
| name | STRING | TEXT |
| company_id | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "Updated Organization",
  "name" : "updatedOrganization",
  "type" : "pipedrive/v1/updatedOrganization"
}
```


### Updated Person
Name: updatedPerson

Trigger off whenever an existing person is updated.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| email_messages_count | INTEGER | INTEGER |
| cc_email | STRING | TEXT |
| owner_id | INTEGER | INTEGER |
| id | INTEGER | INTEGER |
| owner_name | STRING | TEXT |
| phone | ARRAY <details> <summary> Items </summary> [{STRING\(value), BOOLEAN\(primary)}] </details> | ARRAY_BUILDER |
| name | STRING | TEXT |
| email | ARRAY <details> <summary> Items </summary> [{STRING\(value), BOOLEAN\(primary)}] </details> | ARRAY_BUILDER |
| company_id | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "Updated Person",
  "name" : "updatedPerson",
  "type" : "pipedrive/v1/updatedPerson"
}
```


<hr />

