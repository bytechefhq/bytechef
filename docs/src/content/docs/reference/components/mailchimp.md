---
title: "Mailchimp"
description: "Mailchimp is a marketing automation and email marketing platform."
---
## Reference
<hr />

Mailchimp is a marketing automation and email marketing platform.


Categories: [MARKETING_AUTOMATION]


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


### Subscribe
Triggers when an Audience subscriber is added to the list.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| List Id | STRING | SELECT  |  The list id of intended audience to which you would like to add the contact.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(email), STRING(email_type), STRING(id), STRING(ip_opt), STRING(ip_signup), STRING(list_id), {STRING(EMAIL), STRING(FNAME), STRING(INTERESTS), STRING(LNAME)}(merges)} | OBJECT_BUILDER  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |






<hr />



## Actions


### Add a new member to the list
Add a new member to the list.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| List Id | STRING | SELECT  |  The unique ID for the list.  |
| Skip Merge Validation | BOOLEAN | SELECT  |  If skip_merge_validation is true, member data will be accepted without merge field values, even if the merge field is usually required. This defaults to false.  |
| Item | {STRING(email_address), STRING(status), STRING(email_type), {}(merge_fields), {}(interests), STRING(language), BOOLEAN(vip), {NUMBER(latitude), NUMBER(longitude)}(location), [{STRING(marketing_permission_id), BOOLEAN(enabled)}](marketing_permissions), STRING(ip_signup), STRING(timestamp_signup), STRING(ip_opt), STRING(timestamp_opt), [STRING](tags)} | OBJECT_BUILDER  |  |


### Output


___Sample Output:___

```{unsubscribe_reason=string, status=subscribed, email_address=string, last_note={note=string, created_at=2019-08-24T14:15:22, note_id=0, created_by=string}, contact_id=string, stats={ecommerce_data={currency_code=USD, number_of_orders=0, total_revenue=0}, avg_open_rate=0, avg_click_rate=0}, merge_fields={property2=, property1=}, full_name=string, list_id=string, tags_count=0, unique_email_id=string, email_client=string, consents_to_one_to_one_messaging=true, source=string, last_changed=2019-08-24T14:15:22, vip=true, member_rating=0, web_id=0, _links=[{rel=string, href=string, schema=string, targetSchema=string, method=GET}], id=string, timestamp_signup=2019-08-24T14:15:22, interests={property2=true, property1=true}, language=string, email_type=string, marketing_permissions=[{enabled=true, marketing_permission_id=string, text=string}], tags=[{name=string, id=0}], ip_signup=string, location={gmtoff=0, country_code=string, timezone=string, dstoff=0, latitude=0, region=string, longitude=0}, ip_opt=string, timestamp_opt=2019-08-24T14:15:22}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | SELECT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| {} | OBJECT_BUILDER  |
| {} | OBJECT_BUILDER  |
| {NUMBER(avg_open_rate), NUMBER(avg_click_rate), {NUMBER(total_revenue), NUMBER(number_of_orders), STRING(currency_code)}(ecommerce_data)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| {NUMBER(latitude), NUMBER(longitude), INTEGER(gmtoff), INTEGER(dstoff), STRING(country_code), STRING(timezone), STRING(region)} | OBJECT_BUILDER  |
| [{STRING(marketing_permission_id), STRING(text), BOOLEAN(enabled)}] | ARRAY_BUILDER  |
| {INTEGER(note_id), STRING(created_at), STRING(created_by), STRING(note)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| {INTEGER(id), STRING(name)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| [{STRING(rel), STRING(href), STRING(method), STRING(targetSchema), STRING(schema)}] | ARRAY_BUILDER  |





