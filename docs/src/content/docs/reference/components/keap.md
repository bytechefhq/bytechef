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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |





<hr />



## Actions


### Create Company
Name: createCompany

Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Item | OBJECT <details> <summary> Properties </summary> {{{STRING\(country_code), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(region), STRING\(zip_code), STRING\(zip_four)}\(address), STRING\(company_name), [{{}\(content), INTEGER\(id)}]\(custom_fields), STRING\(email_address), {STRING\(number), STRING\(type)}\(fax_number), STRING\(notes), STRING\(opt_in_reason), {STRING\(extension), STRING\(number), STRING\(type)}\(phone_number), STRING\(website)}\(address)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| address | OBJECT <details> <summary> Properties </summary> {STRING\(country_code), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(region), STRING\(zip_code), STRING\(zip_four)} </details> | OBJECT_BUILDER |
| company_name | STRING | TEXT |
| custom_fields | ARRAY <details> <summary> Items </summary> [{{}\(content), INTEGER\(id)}] </details> | ARRAY_BUILDER |
| email_address | STRING | TEXT |
| email_opted_in | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| email_status | STRING <details> <summary> Options </summary> UnengagedMarketable, SingleOptIn, DoubleOptin, Confirmed, UnengagedNonMarketable, NonMarketable, Lockdown, Bounce, HardBounce, Manual, Admin, System, ListUnsubscribe, Feedback, Spam, Invalid, Deactivated </details> | SELECT |
| fax_number | OBJECT <details> <summary> Properties </summary> {STRING\(number), STRING\(type)} </details> | OBJECT_BUILDER |
| id | INTEGER | INTEGER |
| notes | STRING | TEXT |
| phone_number | OBJECT <details> <summary> Properties </summary> {STRING\(extension), STRING\(number), STRING\(type)} </details> | OBJECT_BUILDER |
| website | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create Company",
  "name" : "createCompany",
  "parameters" : {
    "__item" : {
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
    }
  },
  "type" : "keap/v1/createCompany"
}
```


### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | Task | OBJECT <details> <summary> Properties </summary> {BOOLEAN\(completed), DATE_TIME\(completion_date), {STRING\(email), STRING\(first_name), INTEGER\(id), STRING\(last_name)}\(contact), DATE_TIME\(creation_date), STRING\(description), DATE_TIME\(due_date), INTEGER\(funnel_id), INTEGER\(jgraph_id), DATE_TIME\(modification_date), INTEGER\(priority), INTEGER\(remind_time), STRING\(title), STRING\(type), STRING\(url), INTEGER\(user_id)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| completed | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| completion_date | DATE_TIME | DATE_TIME |
| contact | OBJECT <details> <summary> Properties </summary> {STRING\(email), STRING\(first_name), INTEGER\(id), STRING\(last_name)} </details> | OBJECT_BUILDER |
| creation_date | DATE_TIME | DATE_TIME |
| description | STRING | TEXT |
| due_date | DATE_TIME | DATE_TIME |
| funnel_id | INTEGER | INTEGER |
| jgraph_id | INTEGER | INTEGER |
| modification_date | DATE_TIME | DATE_TIME |
| priority | INTEGER | INTEGER |
| remind_time | INTEGER | INTEGER |
| title | STRING | TEXT |
| type | STRING | TEXT |
| url | STRING | TEXT |
| user_id | INTEGER | INTEGER |




#### JSON Example
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "__item" : {
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
    }
  },
  "type" : "keap/v1/createTask"
}
```


### Create Contact
Name: createContact

Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| __item | CreateOrPatchContact | OBJECT <details> <summary> Properties </summary> {[{STRING\(country_code), STRING\(field), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(postal_code), STRING\(region), STRING\(zip_code), STRING\(zip_four)}]\(addresses), DATE_TIME\(anniversary), DATE_TIME\(birthday), {INTEGER\(id)}\(company), STRING\(contact_type), [{{}\(content), INTEGER\(id)}]\(custom_fields), [{STRING\(email), STRING\(field)}]\(email_addresses), STRING\(family_name), [{STRING\(field), STRING\(number), STRING\(type)}]\(fax_numbers), STRING\(given_name), STRING\(job_title), INTEGER\(lead_source_id), STRING\(middle_name), STRING\(opt_in_reason), {STRING\(ip_address)}\(origin), INTEGER\(owner_id), [{STRING\(extension), STRING\(field), STRING\(number), STRING\(type)}]\(phone_numbers), STRING\(preferred_locale), STRING\(preferred_name), STRING\(prefix), [{STRING\(name), STRING\(type)}]\(social_accounts), STRING\(source_type), STRING\(spouse_name), STRING\(suffix), STRING\(time_zone), STRING\(website)} </details> | OBJECT_BUILDER |  | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| ScoreValue | STRING | TEXT |
| addresses | ARRAY <details> <summary> Items </summary> [{STRING\(country_code), STRING\(field), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(postal_code), STRING\(region), STRING\(zip_code), STRING\(zip_four)}] </details> | ARRAY_BUILDER |
| anniversary | DATE_TIME | DATE_TIME |
| birthday | DATE_TIME | DATE_TIME |
| company | OBJECT <details> <summary> Properties </summary> {STRING\(company_name), INTEGER\(id)} </details> | OBJECT_BUILDER |
| company_name | STRING | TEXT |
| contact_type | STRING | TEXT |
| custom_fields | ARRAY <details> <summary> Items </summary> [{{}\(content), INTEGER\(id)}] </details> | ARRAY_BUILDER |
| date_created | DATE_TIME | DATE_TIME |
| email_addresses | ARRAY <details> <summary> Items </summary> [{STRING\(email), STRING\(field)}] </details> | ARRAY_BUILDER |
| email_opted_in | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| email_status | STRING <details> <summary> Options </summary> UnengagedMarketable, SingleOptIn, DoubleOptin, Confirmed, UnengagedNonMarketable, NonMarketable, Lockdown, Bounce, HardBounce, Manual, Admin, System, ListUnsubscribe, Feedback, Spam, Invalid, Deactivated </details> | SELECT |
| family_name | STRING | TEXT |
| fax_numbers | ARRAY <details> <summary> Items </summary> [{STRING\(field), STRING\(number), STRING\(type)}] </details> | ARRAY_BUILDER |
| given_name | STRING | TEXT |
| id | INTEGER | INTEGER |
| job_title | STRING | TEXT |
| last_updated | DATE_TIME | DATE_TIME |
| lead_source_id | INTEGER | INTEGER |
| middle_name | STRING | TEXT |
| opt_in_reason | STRING | TEXT |
| origin | OBJECT <details> <summary> Properties </summary> {DATE_TIME\(date), STRING\(ip_address)} </details> | OBJECT_BUILDER |
| owner_id | INTEGER | INTEGER |
| phone_numbers | ARRAY <details> <summary> Items </summary> [{STRING\(extension), STRING\(field), STRING\(number), STRING\(type)}] </details> | ARRAY_BUILDER |
| preferred_locale | STRING | TEXT |
| preferred_name | STRING | TEXT |
| prefix | STRING | TEXT |
| relationships | ARRAY <details> <summary> Items </summary> [{INTEGER\(id), INTEGER\(linked_contact_id), INTEGER\(relationship_type_id)}] </details> | ARRAY_BUILDER |
| social_accounts | ARRAY <details> <summary> Items </summary> [{STRING\(name), STRING\(type)}] </details> | ARRAY_BUILDER |
| source_type | STRING <details> <summary> Options </summary> APPOINTMENT, FORMAPIHOSTED, FORMAPIINTERNAL, WEBFORM, INTERNALFORM, LANDINGPAGE, IMPORT, MANUAL, API, OTHER, UNKNOWN </details> | SELECT |
| spouse_name | STRING | TEXT |
| suffix | STRING | TEXT |
| tag_ids | ARRAY <details> <summary> Items </summary> [INTEGER] </details> | ARRAY_BUILDER |
| time_zone | STRING | TEXT |
| website | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create Contact",
  "name" : "createContact",
  "parameters" : {
    "__item" : {
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
    }
  },
  "type" : "keap/v1/createContact"
}
```




