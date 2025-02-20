---
title: "Mailchimp"
description: "Mailchimp is a marketing automation and email marketing platform."
---

Mailchimp is a marketing automation and email marketing platform.


Categories: marketing-automation


Type: mailchimp/v1

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


### Add Member to List
Name: addMemberToList

Adds a new member to the list.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| listId | List ID | STRING | SELECT | The unique ID for the list. | true |
| skip_merge_validation | Skip Merge Validation | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | If skip_merge_validation is true, member data will be accepted without merge field values, even if the merge field is usually required. This defaults to false. | false |
| __item | Item | OBJECT <details> <summary> Properties </summary> {STRING\(email_address), STRING\(status), STRING\(email_type), {}\(merge_fields), {}\(interests), STRING\(language), BOOLEAN\(vip), {NUMBER\(latitude), NUMBER\(longitude)}\(location), [{STRING\(marketing_permission_id), BOOLEAN\(enabled)}]\(marketing_permissions), STRING\(ip_signup), STRING\(timestamp_signup), STRING\(ip_opt), STRING\(timestamp_opt), [STRING]\(tags)} </details> | OBJECT_BUILDER |  | null |


#### Output


___Sample Output:___

```{timestamp_opt=2019-08-24T14:15:22, unsubscribe_reason=string, status=subscribed, email_address=string, last_note={note=string, created_at=2019-08-24T14:15:22, note_id=0, created_by=string}, contact_id=string, stats={ecommerce_data={currency_code=USD, number_of_orders=0, total_revenue=0}, avg_open_rate=0, avg_click_rate=0}, merge_fields={property2=, property1=}, full_name=string, list_id=string, tags_count=0, unique_email_id=string, email_client=string, consents_to_one_to_one_messaging=true, source=string, last_changed=2019-08-24T14:15:22, vip=true, member_rating=0, web_id=0, _links=[{rel=string, href=string, schema=string, targetSchema=string, method=GET}], id=string, timestamp_signup=2019-08-24T14:15:22, interests={property2=true, property1=true}, language=string, email_type=string, marketing_permissions=[{enabled=true, marketing_permission_id=string, text=string}], tags=[{name=string, id=0}], ip_signup=string, location={longitude=0, gmtoff=0, country_code=string, timezone=string, dstoff=0, latitude=0, region=string}, ip_opt=string}```



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| email_address | STRING | TEXT |
| unique_email_id | STRING | TEXT |
| contact_id | STRING | TEXT |
| full_name | STRING | TEXT |
| web_id | STRING | TEXT |
| email_type | STRING | TEXT |
| status | STRING <details> <summary> Options </summary> subscribed, unsubscribed, cleaned, pending, transactional </details> | SELECT |
| unsubscribe_reason | STRING | TEXT |
| consents_to_one_to_one_messaging | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| merge_fields | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER |
| interests | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER |
| stats | OBJECT <details> <summary> Properties </summary> {NUMBER\(avg_open_rate), NUMBER\(avg_click_rate), {NUMBER\(total_revenue), NUMBER\(number_of_orders), STRING\(currency_code)}\(ecommerce_data)} </details> | OBJECT_BUILDER |
| ip_signup | STRING | TEXT |
| timestamp_signup | STRING | TEXT |
| ip_opt | STRING | TEXT |
| timestamp_opt | STRING | TEXT |
| member_rating | INTEGER | INTEGER |
| last_changed | STRING | TEXT |
| language | STRING | TEXT |
| vip | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |
| email_client | STRING | TEXT |
| location | OBJECT <details> <summary> Properties </summary> {NUMBER\(latitude), NUMBER\(longitude), INTEGER\(gmtoff), INTEGER\(dstoff), STRING\(country_code), STRING\(timezone), STRING\(region)} </details> | OBJECT_BUILDER |
| marketing_permissions | ARRAY <details> <summary> Items </summary> [{STRING\(marketing_permission_id), STRING\(text), BOOLEAN\(enabled)}] </details> | ARRAY_BUILDER |
| last_note | OBJECT <details> <summary> Properties </summary> {INTEGER\(note_id), STRING\(created_at), STRING\(created_by), STRING\(note)} </details> | OBJECT_BUILDER |
| source | STRING | TEXT |
| tags_count | INTEGER | INTEGER |
| tags | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> | OBJECT_BUILDER |
| list_id | STRING | TEXT |
| _links | ARRAY <details> <summary> Items </summary> [{STRING\(rel), STRING\(href), STRING\(method), STRING\(targetSchema), STRING\(schema)}] </details> | ARRAY_BUILDER |




#### JSON Example
```json
{
  "label" : "Add Member to List",
  "name" : "addMemberToList",
  "parameters" : {
    "listId" : "",
    "skip_merge_validation" : false,
    "__item" : {
      "email_address" : "",
      "status" : "",
      "email_type" : "",
      "merge_fields" : { },
      "interests" : { },
      "language" : "",
      "vip" : false,
      "location" : {
        "latitude" : 0.0,
        "longitude" : 0.0
      },
      "marketing_permissions" : [ {
        "marketing_permission_id" : "",
        "enabled" : false
      } ],
      "ip_signup" : "",
      "timestamp_signup" : "",
      "ip_opt" : "",
      "timestamp_opt" : "",
      "tags" : [ "" ]
    }
  },
  "type" : "mailchimp/v1/addMemberToList"
}
```




## Triggers


### Subscribe
Name: subscribe

Triggers when an Audience subscriber is added to the list.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| listId | List Id | STRING | SELECT | The list id of intended audience to which you would like to add the contact. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| data | OBJECT <details> <summary> Properties </summary> {STRING\(email), STRING\(email_type), STRING\(id), STRING\(ip_opt), STRING\(ip_signup), STRING\(list_id), {STRING\(EMAIL), STRING\(FNAME), STRING\(INTERESTS), STRING\(LNAME)}\(merges)} </details> | OBJECT_BUILDER |
| fired_at | DATE_TIME | DATE_TIME |
| type | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Subscribe",
  "name" : "subscribe",
  "parameters" : {
    "listId" : ""
  },
  "type" : "mailchimp/v1/subscribe"
}
```


<hr />

