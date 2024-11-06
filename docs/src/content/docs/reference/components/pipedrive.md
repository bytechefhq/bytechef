---
title: "Pipedrive"
description: "The first CRM designed by salespeople, for salespeople. Do more to grow your business."
---
## Reference
<hr />

The first CRM designed by salespeople, for salespeople. Do more to grow your business.


Categories: [crm]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />



## Triggers


### New Activity
Trigger off whenever a new activity is added.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |







### New Deal
Trigger off whenever a new deal is added.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |







### New Organization
Trigger off whenever a new organization is added.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |







### New Person
Trigger off whenever a new person is added.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| [{STRING\(value), BOOLEAN\(primary)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| [{STRING\(value), BOOLEAN\(primary)}] | ARRAY_BUILDER  |
| INTEGER | INTEGER  |







### Updated Deal
Trigger off whenever an existing deal is updated.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |







### Updated Organization
Trigger off whenever an existing organization is updated.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |







### Updated Person
Trigger off whenever an existing person is updated.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| [{STRING\(value), BOOLEAN\(primary)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| [{STRING\(value), BOOLEAN\(primary)}] | ARRAY_BUILDER  |
| INTEGER | INTEGER  |







<hr />



## Actions


### Get Deals
Returns all deals.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| User | INTEGER | SELECT  |  Deals matching the given user will be returned. However, `filter_id` and `owned_by_you` takes precedence over `user_id` when supplied.  |
| Filter | INTEGER | SELECT  |  Filter to use.  |
| Stage | INTEGER | SELECT  |  Deals within the given stage will be returned.  |
| Status | STRING | SELECT  |  |
| Sort | STRING | TEXT  |  The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys).  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{INTEGER\(id), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}]\(data)} | OBJECT_BUILDER  |






### Add Deal
Adds a new deal.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Deal | {STRING\(title), STRING\(value), STRING\(currency), INTEGER\(user_id), INTEGER\(person_id), INTEGER\(org_id), INTEGER\(pipeline_id), INTEGER\(stage_id), STRING\(status), DATE\(expected_close_date), NUMBER\(probability), STRING\(lost_reason)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(id), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}\(data)} | OBJECT_BUILDER  |






### Search Deals
Searches all deals by title, notes and/or custom fields.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |
| Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |
| Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |
| Person | INTEGER | SELECT  |  Will filter deals by the provided person.  |
| Organization | INTEGER | SELECT  |  Will filter deals by the provided organization.  |
| Status | STRING | SELECT  |  Will filter deals by the provided specific status.  |
| Include Fields | STRING | SELECT  |  Supports including optional fields in the results which are not provided by default.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{[{STRING\(id), STRING\(type), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}]\(items)}\(data)} | OBJECT_BUILDER  |






### Delete Deal
Marks a deal as deleted. After 30 days, the deal will be permanently deleted.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Deal | INTEGER | SELECT  |  Deal to delete  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(id)}\(data)} | OBJECT_BUILDER  |






### Get Details of Deal
Returns the details of a specific deal.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Deal | INTEGER | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(id), {INTEGER\(id), STRING\(name), STRING\(email)}\(user_id), {STRING\(name)}\(person_id), {STRING\(name), STRING\(owner_id)}\(org_id), INTEGER\(stage_id), STRING\(title), INTEGER\(value), STRING\(currency), STRING\(status)}\(data)} | OBJECT_BUILDER  |






### Get Leads
Returns multiple leads. Leads are sorted by the time they were created, from oldest to newest.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Archived Status | STRING | SELECT  |  Filtering based on the archived status of a lead.  |
| Owner | INTEGER | SELECT  |  Leads matching the given user will be returned. However, `filter_id` takes precedence over `owner_id` when supplied.  |
| Person | INTEGER | SELECT  |  If supplied, only leads matching the given person will be returned. However, `filter_id` takes precedence over `person_id` when supplied.  |
| Organization | INTEGER | SELECT  |  If supplied, only leads matching the given organization will be returned. However, `filter_id` takes precedence over `organization_id` when supplied.  |
| Filter | INTEGER | SELECT  |  Filter to use  |
| Sort | STRING | SELECT  |  The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys).  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}]\(data)} | OBJECT_BUILDER  |






### Add Lead
Creates a lead. A lead always has to be linked to a person or an organization or both.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Lead | {STRING\(title), INTEGER\(owner_id), [STRING]\(label_ids), INTEGER\(person_id), INTEGER\(organization_id), {NUMBER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}\(data)} | OBJECT_BUILDER  |






### Delete Lead
Deletes a specific lead.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Lead | STRING | SELECT  |  The ID of the lead  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{STRING\(id)}\(data)} | OBJECT_BUILDER  |






### Get Lead Details
Returns details of a specific lead. 

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Lead | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}\(data)} | OBJECT_BUILDER  |






### Search Leads
Searches all leads by title, notes and/or custom fields.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |
| Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |
| Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |
| Person | INTEGER | SELECT  |  Will filter leads by the provided person ID.  |
| Organization | INTEGER | SELECT  |  Will filter leads by the provided organization ID.  |
| Include Fields | STRING | SELECT  |  Supports including optional fields in the results which are not provided by default.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{[{STRING\(id), STRING\(title), INTEGER\(owner_id), {INTEGER\(amount), STRING\(currency)}\(value), DATE\(expected_close_date), INTEGER\(person_id)}]\(items)}\(data)} | OBJECT_BUILDER  |






### Get All Organizations
Returns all organizations.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Owner | INTEGER | SELECT  |  Organizations owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied.  |
| Filter | INTEGER | SELECT  |  Filter to use  |
| First Characters | STRING | TEXT  |  Organizations whose name starts with the specified letter will be returned (case insensitive)  |
| Sort | STRING | TEXT  |  The field names and sorting mode separated by a comma (`field_name_1ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}]\(data)} | OBJECT_BUILDER  |






### Add Organization
Adds a new organization.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Organization | {STRING\(name), INTEGER\(owner_id)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}\(data)} | OBJECT_BUILDER  |






### Search Organizations
Searches all organizations by name, address, notes and/or custom fields. This endpoint is a wrapper of <a href="https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem">/v1/itemSearch</a> with a narrower OAuth scope.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |
| Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |
| Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}]\(items)}\(data)} | OBJECT_BUILDER  |






### Delete Organization
Marks an organization as deleted. After 30 days, the organization will be permanently deleted.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Organization | INTEGER | SELECT  |  Organization to delete  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(id)}\(data)} | OBJECT_BUILDER  |






### Get Details of Organization
Returns the details of an organization.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Organizaton | INTEGER | SELECT  |  Organization to get details.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), STRING\(name)}\(data)} | OBJECT_BUILDER  |






### Get Persons
Returns all persons.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Owner | INTEGER | SELECT  |  Persons owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied.  |
| Filter | INTEGER | SELECT  |  Filter to use.  |
| First Characters | STRING | TEXT  |  Persons whose name starts with the specified letter will be returned (case insensitive)  |
| Sort | STRING | TEXT  |  The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys).  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}]\(data)} | OBJECT_BUILDER  |






### Add Person
Adds a new person.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Person | {STRING\(name), INTEGER\(owner_id), INTEGER\(org_id), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}\(data)} | OBJECT_BUILDER  |






### Search Persons
Searches all persons by name, email, phone, notes and/or custom fields.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |
| Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |
| Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |
| Organization | INTEGER | SELECT  |  Will filter persons by the provided organization.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{[{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}]\(items)}\(data)} | OBJECT_BUILDER  |






### Delete Person
Marks a person as deleted. After 30 days, the person will be permanently deleted.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Person | INTEGER | SELECT  |  Person to delete  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(id)}\(data)} | OBJECT_BUILDER  |






### Get Details of Person
Returns the details of a person. This also returns some additional fields which are not present when asking for all persons.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Person | INTEGER | SELECT  |  Person to get details  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER\(id), INTEGER\(company_id), {INTEGER\(id), STRING\(name), STRING\(email)}\(owner_id), {STRING\(name), INTEGER\(owner_id), STRING\(cc_email)}\(org_id), STRING\(name), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(phone), [{STRING\(value), BOOLEAN\(primary), STRING\(label)}]\(email)}\(data)} | OBJECT_BUILDER  |






