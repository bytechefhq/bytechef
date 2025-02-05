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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Get Deals
Returns all deals.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| user_id | User ID | INTEGER | SELECT  |  Deals matching the given user will be returned. However, `filter_id` and `owned_by_you` takes precedence over `user_id` when supplied.  |  false  |
| filter_id | Filter ID | INTEGER | SELECT  |  ID of the filter to use.  |  false  |
| stage_id | Stage ID | INTEGER | SELECT  |  Deals within the given stage will be returned.  |  false  |
| status | Status | STRING | SELECT  |  | false  |
| sort | Sort | STRING | TEXT  |  The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys).  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {[{INTEGER\(id), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}]\(data)} | OBJECT_BUILDER  |






### Add Deal
Adds a new deal.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Deal | {STRING\(title), STRING\(value), STRING\(currency), INTEGER\(user_id), INTEGER\(person_id), INTEGER\(org_id), INTEGER\(pipeline_id), INTEGER\(stage_id), STRING\(status), DATE\(expected_close_date), NUMBER\(probability), STRING\(lost_reason)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}\(data)} | OBJECT_BUILDER  |






### Search Deals
Searches all deals by title, notes and/or custom fields.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| term | Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |  true  |
| fields | Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |  false  |
| exact_match | Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |  false  |
| person_id | Person ID | INTEGER | SELECT  |  Will filter deals by the provided person.  |  false  |
| organization_id | Organization ID | INTEGER | SELECT  |  Will filter deals by the provided organization.  |  false  |
| status | Status | STRING | SELECT  |  Will filter deals by the provided specific status.  |  false  |
| include_fields | Include Fields | STRING | SELECT  |  Supports including optional fields in the results which are not provided by default.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{[{STRING\(id), STRING\(type), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}]\(items)}\(data)} | OBJECT_BUILDER  |






### Delete Deal
Marks a deal as deleted. After 30 days, the deal will be permanently deleted.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Deal ID | INTEGER | SELECT  |  Id of the deal to delete.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id)}\(data)} | OBJECT_BUILDER  |






### Get Details of Deal
Returns the details of a specific deal.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Deal | INTEGER | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}\(data)} | OBJECT_BUILDER  |






### Get Leads
Returns multiple leads. Leads are sorted by the time they were created, from oldest to newest.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| archived_status | Archived Status | STRING | SELECT  |  Filtering based on the archived status of a lead.  |  false  |
| owner_id | Owner iD | INTEGER | SELECT  |  Leads matching the given user will be returned. However, `filter_id` takes precedence over `owner_id` when supplied.  |  false  |
| person_id | Person ID | INTEGER | SELECT  |  If supplied, only leads matching the given person will be returned. However, `filter_id` takes precedence over `person_id` when supplied.  |  false  |
| organization_id | Organization ID | INTEGER | SELECT  |  If supplied, only leads matching the given organization will be returned. However, `filter_id` takes precedence over `organization_id` when supplied.  |  false  |
| filter_id | Filter ID | INTEGER | SELECT  |  Filter to use  |  false  |
| sort | Sort | STRING | SELECT  |  The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys).  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {[{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}]\(data)} | OBJECT_BUILDER  |






### Add Lead
Creates a lead. A lead always has to be linked to a person or an organization or both.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Lead | {STRING\(title), INTEGER\(owner_id), [STRING]\(label_ids), INTEGER\(person_id), INTEGER\(organization_id), {NUMBER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}\(data)} | OBJECT_BUILDER  |






### Delete Lead
Deletes a specific lead.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Lead ID | STRING | SELECT  |  The ID of the lead  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{STRING\(id)}\(data)} | OBJECT_BUILDER  |






### Get Lead Details
Returns details of a specific lead. 

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Lead ID | STRING | SELECT  |  | true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}\(data)} | OBJECT_BUILDER  |






### Search Leads
Searches all leads by title, notes and/or custom fields.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| term | Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |  true  |
| fields | Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |  false  |
| exact_match | Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |  false  |
| person_id | Person ID | INTEGER | SELECT  |  Will filter leads by the provided person ID.  |  false  |
| organization_id | Organization ID | INTEGER | SELECT  |  Will filter leads by the provided organization ID.  |  false  |
| include_fields | Include Fields | STRING | SELECT  |  Supports including optional fields in the results which are not provided by default.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{[{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}]\(items)}\(data)} | OBJECT_BUILDER  |






### Get All Organizations
Returns all organizations.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| user_id | User ID | INTEGER | SELECT  |  Organizations owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied.  |  false  |
| filter_id | Filter ID | INTEGER | SELECT  |  Filter to use  |  false  |
| first_char | First Characters | STRING | TEXT  |  Organizations whose name starts with the specified letter will be returned (case insensitive)  |  false  |
| sort | Sort | STRING | TEXT  |  The field names and sorting mode separated by a comma (`field_name_1ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}]\(data)} | OBJECT_BUILDER  |






### Add Organization
Adds a new organization.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Organization | {STRING\(name), INTEGER\(owner_id)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}\(data)} | OBJECT_BUILDER  |






### Search Organizations
Searches all organizations by name, address, notes and/or custom fields. This endpoint is a wrapper of <a href="https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem">/v1/itemSearch</a> with a narrower OAuth scope.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| term | Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |  true  |
| fields | Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |  false  |
| exact_match | Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}]\(items)}\(data)} | OBJECT_BUILDER  |






### Delete Organization
Marks an organization as deleted. After 30 days, the organization will be permanently deleted.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Organization | INTEGER | SELECT  |  Organization to delete  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id)}\(data)} | OBJECT_BUILDER  |






### Get Details of Organization
Returns the details of an organization.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Organizaton | INTEGER | SELECT  |  Organization to get details.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}\(data)} | OBJECT_BUILDER  |






### Get Persons
Returns all persons.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| user_id | User ID | INTEGER | SELECT  |  Persons owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied.  |  false  |
| filter_id | Filter ID | INTEGER | SELECT  |  Filter to use.  |  false  |
| first_char | First Characters | STRING | TEXT  |  Persons whose name starts with the specified letter will be returned (case insensitive)  |  false  |
| sort | Sort | STRING | TEXT  |  The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys).  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}]\(data)} | OBJECT_BUILDER  |






### Add Person
Adds a new person.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Person | {STRING\(name), INTEGER\(owner_id), INTEGER\(org_id), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}\(data)} | OBJECT_BUILDER  |






### Search Persons
Searches all persons by name, email, phone, notes and/or custom fields.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| term | Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |  true  |
| fields | Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |  false  |
| exact_match | Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |  false  |
| organization_id | Organization ID | INTEGER | SELECT  |  Will filter persons by the provided organization.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}]\(items)}\(data)} | OBJECT_BUILDER  |






### Delete Person
Marks a person as deleted. After 30 days, the person will be permanently deleted.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Person | INTEGER | SELECT  |  Person to delete  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id)}\(data)} | OBJECT_BUILDER  |






### Get Details of Person
Returns the details of a person. This also returns some additional fields which are not present when asking for all persons.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| id | Person | INTEGER | SELECT  |  Person to get details  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| body | {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}\(data)} | OBJECT_BUILDER  |








## Triggers


### New Activity
Trigger off whenever a new activity is added.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| type_name | STRING | TEXT  |
| public_description | STRING | TEXT  |
| subject | STRING | TEXT  |
| type | STRING | TEXT  |
| id | INTEGER | INTEGER  |
| owner_name | STRING | TEXT  |
| user_id | INTEGER | INTEGER  |
| company_id | INTEGER | INTEGER  |







### New Deal
Trigger off whenever a new deal is added.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| email_messages_count | INTEGER | INTEGER  |
| cc_email | STRING | TEXT  |
| id | INTEGER | INTEGER  |
| person_id | INTEGER | INTEGER  |
| owner_name | STRING | TEXT  |
| status | STRING | TEXT  |
| title | STRING | TEXT  |
| currency | STRING | TEXT  |
| value | INTEGER | INTEGER  |







### New Organization
Trigger off whenever a new organization is added.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| email_messages_count | INTEGER | INTEGER  |
| cc_email | STRING | TEXT  |
| owner_id | INTEGER | INTEGER  |
| id | INTEGER | INTEGER  |
| owner_name | STRING | TEXT  |
| name | STRING | TEXT  |
| company_id | INTEGER | INTEGER  |







### New Person
Trigger off whenever a new person is added.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| email_messages_count | INTEGER | INTEGER  |
| cc_email | STRING | TEXT  |
| owner_id | INTEGER | INTEGER  |
| id | INTEGER | INTEGER  |
| owner_name | STRING | TEXT  |
| phone | [{STRING\(value), BOOLEAN\(primary)}] | ARRAY_BUILDER  |
| name | STRING | TEXT  |
| email | [{STRING\(value), BOOLEAN\(primary)}] | ARRAY_BUILDER  |
| company_id | INTEGER | INTEGER  |







### Updated Deal
Trigger off whenever an existing deal is updated.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| email_messages_count | INTEGER | INTEGER  |
| cc_email | STRING | TEXT  |
| id | INTEGER | INTEGER  |
| person_id | INTEGER | INTEGER  |
| owner_name | STRING | TEXT  |
| status | STRING | TEXT  |
| title | STRING | TEXT  |
| currency | STRING | TEXT  |
| value | INTEGER | INTEGER  |







### Updated Organization
Trigger off whenever an existing organization is updated.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| email_messages_count | INTEGER | INTEGER  |
| cc_email | STRING | TEXT  |
| owner_id | INTEGER | INTEGER  |
| id | INTEGER | INTEGER  |
| owner_name | STRING | TEXT  |
| name | STRING | TEXT  |
| company_id | INTEGER | INTEGER  |







### Updated Person
Trigger off whenever an existing person is updated.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| email_messages_count | INTEGER | INTEGER  |
| cc_email | STRING | TEXT  |
| owner_id | INTEGER | INTEGER  |
| id | INTEGER | INTEGER  |
| owner_name | STRING | TEXT  |
| phone | [{STRING\(value), BOOLEAN\(primary)}] | ARRAY_BUILDER  |
| name | STRING | TEXT  |
| email | [{STRING\(value), BOOLEAN\(primary)}] | ARRAY_BUILDER  |
| company_id | INTEGER | INTEGER  |







<hr />

