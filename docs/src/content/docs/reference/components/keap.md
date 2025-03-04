---
title: "Keap"
description: "Keap is a customer comprehensive customer relationship management platform designed to help small businesses streamline sales, marketing, and customer management processes in one integrated system."
---

Keap is a customer comprehensive customer relationship management platform designed to help small businesses streamline sales, marketing, and customer management processes in one integrated system.


Categories: crm


Type: keap/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Create Company
Name: createCompany

Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| address | CompanyAddress | OBJECT <details> <summary> Properties </summary> {{STRING\(country_code), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(region), STRING\(zip_code), STRING\(zip_four)}\(address), STRING\(company_name), [{{}\(content), INTEGER\(id)}]\(custom_fields), STRING\(email_address), {STRING\(number), STRING\(type)}\(fax_number), STRING\(notes), STRING\(opt_in_reason), {STRING\(extension), STRING\(number), STRING\(type)}\(phone_number), STRING\(website)} </details> |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| address | OBJECT <details> <summary> Properties </summary> {STRING\(country_code), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(region), STRING\(zip_code), STRING\(zip_four)} </details> |  |
| company_name | STRING |  |
| custom_fields | ARRAY <details> <summary> Items </summary> [{{}\(content), INTEGER\(id)}] </details> |  |
| email_address | STRING |  |
| email_opted_in | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |
| email_status | STRING <details> <summary> Options </summary> UnengagedMarketable, SingleOptIn, DoubleOptin, Confirmed, UnengagedNonMarketable, NonMarketable, Lockdown, Bounce, HardBounce, Manual, Admin, System, ListUnsubscribe, Feedback, Spam, Invalid, Deactivated </details> |  |
| fax_number | OBJECT <details> <summary> Properties </summary> {STRING\(number), STRING\(type)} </details> |  |
| id | INTEGER |  |
| notes | STRING |  |
| phone_number | OBJECT <details> <summary> Properties </summary> {STRING\(extension), STRING\(number), STRING\(type)} </details> |  |
| website | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Company",
  "name" : "createCompany",
  "parameters" : {
    "address" : {
      "address" : {
        "country_code" : "",
        "line1" : "",
        "line2" : "",
        "locality" : "",
        "region" : "",
        "zip_code" : "",
        "zip_four" : ""
      },
      "company_name" : "",
      "custom_fields" : [ {
        "content" : { },
        "id" : 1
      } ],
      "email_address" : "",
      "fax_number" : {
        "number" : "",
        "type" : ""
      },
      "notes" : "",
      "opt_in_reason" : "",
      "phone_number" : {
        "extension" : "",
        "number" : "",
        "type" : ""
      },
      "website" : ""
    }
  },
  "type" : "keap/v1/createCompany"
}
```


### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| completed | Completed | BOOLEAN <details> <summary> Options </summary> true, false </details> |  | false |
| completion_date | Completion Date | DATE_TIME |  | false |
| contact | BasicContact | OBJECT <details> <summary> Properties </summary> {STRING\(email), STRING\(first_name), INTEGER\(id), STRING\(last_name)} </details> |  | false |
| creation_date | Creation Date | DATE_TIME |  | false |
| description | Description | STRING |  | false |
| due_date | Due Date | DATE_TIME |  | false |
| funnel_id | Funnel Id | INTEGER |  | false |
| jgraph_id | Jgraph Id | INTEGER |  | false |
| modification_date | Modification Date | DATE_TIME |  | false |
| priority | Priority | INTEGER |  | false |
| remind_time | Remind Time | INTEGER | Value in minutes before start_date to show pop-up reminder. Acceptable values are in [`5`,`10`,`15`,`30`,`60`,`120`,`240`,`480`,`1440`,`2880`] | false |
| title | Title | STRING |  | false |
| type | Type | STRING |  | false |
| url | Url | STRING |  | false |
| user_id | User Id | INTEGER |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| completed | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |
| completion_date | DATE_TIME |  |
| contact | OBJECT <details> <summary> Properties </summary> {STRING\(email), STRING\(first_name), INTEGER\(id), STRING\(last_name)} </details> |  |
| creation_date | DATE_TIME |  |
| description | STRING |  |
| due_date | DATE_TIME |  |
| funnel_id | INTEGER |  |
| jgraph_id | INTEGER |  |
| modification_date | DATE_TIME |  |
| priority | INTEGER |  |
| remind_time | INTEGER | Value in minutes before start_date to show pop-up reminder. Acceptable values are in [`5`,`10`,`15`,`30`,`60`,`120`,`240`,`480`,`1440`,`2880`] |
| title | STRING |  |
| type | STRING |  |
| url | STRING |  |
| user_id | INTEGER |  |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "completed" : false,
    "completion_date" : "2021-01-01T00:00:00",
    "contact" : {
      "email" : "",
      "first_name" : "",
      "id" : 1,
      "last_name" : ""
    },
    "creation_date" : "2021-01-01T00:00:00",
    "description" : "",
    "due_date" : "2021-01-01T00:00:00",
    "funnel_id" : 1,
    "jgraph_id" : 1,
    "modification_date" : "2021-01-01T00:00:00",
    "priority" : 1,
    "remind_time" : 1,
    "title" : "",
    "type" : "",
    "url" : "",
    "user_id" : 1
  },
  "type" : "keap/v1/createTask"
}
```


### Create Contact
Name: createContact

Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| addresses | Addresses | ARRAY <details> <summary> Items </summary> [{STRING\(country_code), STRING\(field), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(postal_code), STRING\(region), STRING\(zip_code), STRING\(zip_four)}] </details> |  | false |
| anniversary | Anniversary | DATE_TIME |  | false |
| birthday | Birthday | DATE_TIME |  | false |
| company | RequestCompanyReference | OBJECT <details> <summary> Properties </summary> {INTEGER\(id)} </details> |  | false |
| contact_type | Contact Type | STRING |  | false |
| custom_fields | Custom Fields | ARRAY <details> <summary> Items </summary> [{{}\(content), INTEGER\(id)}] </details> |  | false |
| email_addresses | Email Addresses | ARRAY <details> <summary> Items </summary> [{STRING\(email), STRING\(field)}] </details> |  | false |
| family_name | Family Name | STRING |  | false |
| fax_numbers | Fax Numbers | ARRAY <details> <summary> Items </summary> [{STRING\(field), STRING\(number), STRING\(type)}] </details> |  | false |
| given_name | Given Name | STRING |  | false |
| job_title | Job Title | STRING |  | false |
| lead_source_id | Lead Source Id | INTEGER |  | false |
| middle_name | Middle Name | STRING |  | false |
| opt_in_reason | Opt In Reason | STRING |  | false |
| origin | CreateContactOrigin | OBJECT <details> <summary> Properties </summary> {STRING\(ip_address)} </details> |  | false |
| owner_id | Owner Id | INTEGER |  | false |
| phone_numbers | Phone Numbers | ARRAY <details> <summary> Items </summary> [{STRING\(extension), STRING\(field), STRING\(number), STRING\(type)}] </details> |  | false |
| preferred_locale | Preferred Locale | STRING |  | false |
| preferred_name | Preferred Name | STRING |  | false |
| prefix | Prefix | STRING |  | false |
| social_accounts | Social Accounts | ARRAY <details> <summary> Items </summary> [{STRING\(name), STRING\(type)}] </details> |  | false |
| source_type | Source Type | STRING <details> <summary> Options </summary> APPOINTMENT, FORMAPIHOSTED, FORMAPIINTERNAL, WEBFORM, INTERNALFORM, LANDINGPAGE, IMPORT, MANUAL, API, OTHER, UNKNOWN </details> |  | false |
| spouse_name | Spouse Name | STRING |  | false |
| suffix | Suffix | STRING |  | false |
| time_zone | Time Zone | STRING |  | false |
| website | Website | STRING |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| ScoreValue | STRING |  |
| addresses | ARRAY <details> <summary> Items </summary> [{STRING\(country_code), STRING\(field), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(postal_code), STRING\(region), STRING\(zip_code), STRING\(zip_four)}] </details> |  |
| anniversary | DATE_TIME |  |
| birthday | DATE_TIME |  |
| company | OBJECT <details> <summary> Properties </summary> {STRING\(company_name), INTEGER\(id)} </details> |  |
| company_name | STRING |  |
| contact_type | STRING |  |
| custom_fields | ARRAY <details> <summary> Items </summary> [{{}\(content), INTEGER\(id)}] </details> |  |
| date_created | DATE_TIME |  |
| email_addresses | ARRAY <details> <summary> Items </summary> [{STRING\(email), STRING\(field)}] </details> |  |
| email_opted_in | BOOLEAN <details> <summary> Options </summary> true, false </details> |  |
| email_status | STRING <details> <summary> Options </summary> UnengagedMarketable, SingleOptIn, DoubleOptin, Confirmed, UnengagedNonMarketable, NonMarketable, Lockdown, Bounce, HardBounce, Manual, Admin, System, ListUnsubscribe, Feedback, Spam, Invalid, Deactivated </details> |  |
| family_name | STRING |  |
| fax_numbers | ARRAY <details> <summary> Items </summary> [{STRING\(field), STRING\(number), STRING\(type)}] </details> |  |
| given_name | STRING |  |
| id | INTEGER |  |
| job_title | STRING |  |
| last_updated | DATE_TIME |  |
| lead_source_id | INTEGER |  |
| middle_name | STRING |  |
| opt_in_reason | STRING |  |
| origin | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(date), STRING\(ip_address)} </details> |  |
| owner_id | INTEGER |  |
| phone_numbers | ARRAY <details> <summary> Items </summary> [{STRING\(extension), STRING\(field), STRING\(number), STRING\(type)}] </details> |  |
| preferred_locale | STRING |  |
| preferred_name | STRING |  |
| prefix | STRING |  |
| relationships | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), INTEGER\(linked_contact_id), INTEGER\(relationship_type_id)}] </details> |  |
| social_accounts | ARRAY <details> <summary> Items </summary> [{STRING\(name), STRING\(type)}] </details> |  |
| source_type | STRING <details> <summary> Options </summary> APPOINTMENT, FORMAPIHOSTED, FORMAPIINTERNAL, WEBFORM, INTERNALFORM, LANDINGPAGE, IMPORT, MANUAL, API, OTHER, UNKNOWN </details> |  |
| spouse_name | STRING |  |
| suffix | STRING |  |
| tag_ids | ARRAY <details> <summary> Items </summary> [INTEGER] </details> |  |
| time_zone | STRING |  |
| website | STRING |  |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "addresses" : [ {
      "country_code" : "",
      "field" : "",
      "line1" : "",
      "line2" : "",
      "locality" : "",
      "postal_code" : "",
      "region" : "",
      "zip_code" : "",
      "zip_four" : ""
    } ],
    "anniversary" : "2021-01-01T00:00:00",
    "birthday" : "2021-01-01T00:00:00",
    "company" : {
      "id" : 1
    },
    "contact_type" : "",
    "custom_fields" : [ {
      "content" : { },
      "id" : 1
    } ],
    "email_addresses" : [ {
      "email" : "",
      "field" : ""
    } ],
    "family_name" : "",
    "fax_numbers" : [ {
      "field" : "",
      "number" : "",
      "type" : ""
    } ],
    "given_name" : "",
    "job_title" : "",
    "lead_source_id" : 1,
    "middle_name" : "",
    "opt_in_reason" : "",
    "origin" : {
      "ip_address" : ""
    },
    "owner_id" : 1,
    "phone_numbers" : [ {
      "extension" : "",
      "field" : "",
      "number" : "",
      "type" : ""
    } ],
    "preferred_locale" : "",
    "preferred_name" : "",
    "prefix" : "",
    "social_accounts" : [ {
      "name" : "",
      "type" : ""
    } ],
    "source_type" : "",
    "spouse_name" : "",
    "suffix" : "",
    "time_zone" : "",
    "website" : ""
  },
  "type" : "keap/v1/createContact"
}
```




