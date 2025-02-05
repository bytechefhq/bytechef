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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Company
Creates a new company.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Item | {{{STRING\(country_code), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(region), STRING\(zip_code), STRING\(zip_four)}\(address), STRING\(company_name), [{{}\(content), INTEGER\(id)}]\(custom_fields), STRING\(email_address), {STRING\(number), STRING\(type)}\(fax_number), STRING\(notes), STRING\(opt_in_reason), {STRING\(extension), STRING\(number), STRING\(type)}\(phone_number), STRING\(website)}\(address)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| address | {STRING\(country_code), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(region), STRING\(zip_code), STRING\(zip_four)} | OBJECT_BUILDER  |
| company_name | STRING | TEXT  |
| custom_fields | [{{}\(content), INTEGER\(id)}] | ARRAY_BUILDER  |
| email_address | STRING | TEXT  |
| email_opted_in | BOOLEAN | SELECT  |
| email_status | STRING | SELECT  |
| fax_number | {STRING\(number), STRING\(type)} | OBJECT_BUILDER  |
| id | INTEGER | INTEGER  |
| notes | STRING | TEXT  |
| phone_number | {STRING\(extension), STRING\(number), STRING\(type)} | OBJECT_BUILDER  |
| website | STRING | TEXT  |






### Create Task
Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | Task | {BOOLEAN\(completed), DATE_TIME\(completion_date), {STRING\(email), STRING\(first_name), INTEGER\(id), STRING\(last_name)}\(contact), DATE_TIME\(creation_date), STRING\(description), DATE_TIME\(due_date), INTEGER\(funnel_id), INTEGER\(jgraph_id), DATE_TIME\(modification_date), INTEGER\(priority), INTEGER\(remind_time), STRING\(title), STRING\(type), STRING\(url), INTEGER\(user_id)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| completed | BOOLEAN | SELECT  |
| completion_date | DATE_TIME | DATE_TIME  |
| contact | {STRING\(email), STRING\(first_name), INTEGER\(id), STRING\(last_name)} | OBJECT_BUILDER  |
| creation_date | DATE_TIME | DATE_TIME  |
| description | STRING | TEXT  |
| due_date | DATE_TIME | DATE_TIME  |
| funnel_id | INTEGER | INTEGER  |
| jgraph_id | INTEGER | INTEGER  |
| modification_date | DATE_TIME | DATE_TIME  |
| priority | INTEGER | INTEGER  |
| remind_time | INTEGER | INTEGER  |
| title | STRING | TEXT  |
| type | STRING | TEXT  |
| url | STRING | TEXT  |
| user_id | INTEGER | INTEGER  |






### Create Contact
Creates a new contact.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| __item | CreateOrPatchContact | {[{STRING\(country_code), STRING\(field), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(postal_code), STRING\(region), STRING\(zip_code), STRING\(zip_four)}]\(addresses), DATE_TIME\(anniversary), DATE_TIME\(birthday), {INTEGER\(id)}\(company), STRING\(contact_type), [{{}\(content), INTEGER\(id)}]\(custom_fields), [{STRING\(email), STRING\(field)}]\(email_addresses), STRING\(family_name), [{STRING\(field), STRING\(number), STRING\(type)}]\(fax_numbers), STRING\(given_name), STRING\(job_title), INTEGER\(lead_source_id), STRING\(middle_name), STRING\(opt_in_reason), {STRING\(ip_address)}\(origin), INTEGER\(owner_id), [{STRING\(extension), STRING\(field), STRING\(number), STRING\(type)}]\(phone_numbers), STRING\(preferred_locale), STRING\(preferred_name), STRING\(prefix), [{STRING\(name), STRING\(type)}]\(social_accounts), STRING\(source_type), STRING\(spouse_name), STRING\(suffix), STRING\(time_zone), STRING\(website)} | OBJECT_BUILDER  |  | null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| ScoreValue | STRING | TEXT  |
| addresses | [{STRING\(country_code), STRING\(field), STRING\(line1), STRING\(line2), STRING\(locality), STRING\(postal_code), STRING\(region), STRING\(zip_code), STRING\(zip_four)}] | ARRAY_BUILDER  |
| anniversary | DATE_TIME | DATE_TIME  |
| birthday | DATE_TIME | DATE_TIME  |
| company | {STRING\(company_name), INTEGER\(id)} | OBJECT_BUILDER  |
| company_name | STRING | TEXT  |
| contact_type | STRING | TEXT  |
| custom_fields | [{{}\(content), INTEGER\(id)}] | ARRAY_BUILDER  |
| date_created | DATE_TIME | DATE_TIME  |
| email_addresses | [{STRING\(email), STRING\(field)}] | ARRAY_BUILDER  |
| email_opted_in | BOOLEAN | SELECT  |
| email_status | STRING | SELECT  |
| family_name | STRING | TEXT  |
| fax_numbers | [{STRING\(field), STRING\(number), STRING\(type)}] | ARRAY_BUILDER  |
| given_name | STRING | TEXT  |
| id | INTEGER | INTEGER  |
| job_title | STRING | TEXT  |
| last_updated | DATE_TIME | DATE_TIME  |
| lead_source_id | INTEGER | INTEGER  |
| middle_name | STRING | TEXT  |
| opt_in_reason | STRING | TEXT  |
| origin | {DATE_TIME\(date), STRING\(ip_address)} | OBJECT_BUILDER  |
| owner_id | INTEGER | INTEGER  |
| phone_numbers | [{STRING\(extension), STRING\(field), STRING\(number), STRING\(type)}] | ARRAY_BUILDER  |
| preferred_locale | STRING | TEXT  |
| preferred_name | STRING | TEXT  |
| prefix | STRING | TEXT  |
| relationships | [{INTEGER\(id), INTEGER\(linked_contact_id), INTEGER\(relationship_type_id)}] | ARRAY_BUILDER  |
| social_accounts | [{STRING\(name), STRING\(type)}] | ARRAY_BUILDER  |
| source_type | STRING | SELECT  |
| spouse_name | STRING | TEXT  |
| suffix | STRING | TEXT  |
| tag_ids | [INTEGER] | ARRAY_BUILDER  |
| time_zone | STRING | TEXT  |
| website | STRING | TEXT  |








## Triggers



<hr />

