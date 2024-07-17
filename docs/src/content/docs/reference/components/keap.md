---
title: "Keap"
description: "Keap is a customer comprehensive customer relationship management platform designed to help small businesses streamline sales, marketing, and customer management processes in one integrated system."
---
## Reference
<hr />

Keap is a customer comprehensive customer relationship management platform designed to help small businesses streamline sales, marketing, and customer management processes in one integrated system.


Categories: [CRM]


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



<hr />



## Actions


### Create a Company
Creates a new company

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Item | {{{STRING(country_code), STRING(line1), STRING(line2), STRING(locality), STRING(region), STRING(zip_code), STRING(zip_four)}(address), STRING(company_name), [{{}(content), INTEGER(id)}](custom_fields), STRING(email_address), {STRING(number), STRING(type)}(fax_number), STRING(notes), STRING(opt_in_reason), {STRING(extension), STRING(number), STRING(type)}(phone_number), STRING(website)}(address)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(country_code), STRING(line1), STRING(line2), STRING(locality), STRING(region), STRING(zip_code), STRING(zip_four)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| [{{}(content), INTEGER(id)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| STRING | SELECT  |
| {STRING(number), STRING(type)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {STRING(extension), STRING(number), STRING(type)} | OBJECT_BUILDER  |
| STRING | TEXT  |





### Create a Task
Creates a new task

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Task | {BOOLEAN(completed), DATE_TIME(completion_date), {STRING(email), STRING(first_name), INTEGER(id), STRING(last_name)}(contact), DATE_TIME(creation_date), STRING(description), DATE_TIME(due_date), INTEGER(funnel_id), INTEGER(jgraph_id), DATE_TIME(modification_date), INTEGER(priority), INTEGER(remind_time), STRING(title), STRING(type), STRING(url), INTEGER(user_id)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| DATE_TIME | DATE_TIME  |
| {STRING(email), STRING(first_name), INTEGER(id), STRING(last_name)} | OBJECT_BUILDER  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| DATE_TIME | DATE_TIME  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |





### Create a Contact
Creates a new contact

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Create Or Patch Contact | {[{STRING(country_code), STRING(field), STRING(line1), STRING(line2), STRING(locality), STRING(postal_code), STRING(region), STRING(zip_code), STRING(zip_four)}](addresses), DATE_TIME(anniversary), DATE_TIME(birthday), {INTEGER(id)}(company), STRING(contact_type), [{{}(content), INTEGER(id)}](custom_fields), [{STRING(email), STRING(field)}](email_addresses), STRING(family_name), [{STRING(field), STRING(number), STRING(type)}](fax_numbers), STRING(given_name), STRING(job_title), INTEGER(lead_source_id), STRING(middle_name), STRING(opt_in_reason), {STRING(ip_address)}(origin), INTEGER(owner_id), [{STRING(extension), STRING(field), STRING(number), STRING(type)}](phone_numbers), STRING(preferred_locale), STRING(preferred_name), STRING(prefix), [{STRING(name), STRING(type)}](social_accounts), STRING(source_type), STRING(spouse_name), STRING(suffix), STRING(time_zone), STRING(website)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| [{STRING(country_code), STRING(field), STRING(line1), STRING(line2), STRING(locality), STRING(postal_code), STRING(region), STRING(zip_code), STRING(zip_four)}] | ARRAY_BUILDER  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| {STRING(company_name), INTEGER(id)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{{}(content), INTEGER(id)}] | ARRAY_BUILDER  |
| DATE_TIME | DATE_TIME  |
| [{STRING(email), STRING(field)}] | ARRAY_BUILDER  |
| BOOLEAN | SELECT  |
| STRING | SELECT  |
| STRING | TEXT  |
| [{STRING(field), STRING(number), STRING(type)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| {DATE_TIME(date), STRING(ip_address)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| [{STRING(extension), STRING(field), STRING(number), STRING(type)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{INTEGER(id), INTEGER(linked_contact_id), INTEGER(relationship_type_id)}] | ARRAY_BUILDER  |
| [{STRING(name), STRING(type)}] | ARRAY_BUILDER  |
| STRING | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [INTEGER] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |





