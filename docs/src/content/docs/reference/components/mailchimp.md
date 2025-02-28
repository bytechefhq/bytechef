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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Add Member to List
Name: addMemberToList

Adds a new member to the list.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| listId | List ID | STRING | The unique ID for the list. | true |
| skip_merge_validation | Skip Merge Validation | BOOLEAN <details> <summary> Options </summary> true, false </details> | If skip_merge_validation is true, member data will be accepted without merge field values, even if the merge field is usually required. This defaults to false. | false |
| email_address | Email Address | STRING | Email address for a subscriber. | true |
| status | Status | STRING <details> <summary> Options </summary> subscribed, unsubscribed, cleaned, pending, transactional </details> | Subscriber's current status. | true |
| email_type | Email Type | STRING <details> <summary> Options </summary> html, text </details> | Type of email this member asked to get ('html' or 'text'). | false |
| merge_fields | Merge Fields | OBJECT <details> <summary> Properties </summary> {} </details> | A dictionary of merge fields where the keys are the merge tags. | false |
| interests | Interests | OBJECT <details> <summary> Properties </summary> {} </details> | The key of this object's properties is the ID of the interest in question. | false |
| language | Language | STRING | If set/detected, the subscriber's language. | false |
| vip | Vip | BOOLEAN <details> <summary> Options </summary> true, false </details> | VIP status for subscriber. | false |
| location | Location | OBJECT <details> <summary> Properties </summary> {NUMBER\(latitude), NUMBER\(longitude)} </details> | Subscriber location information. | false |
| marketing_permissions | Marketing Permissions | ARRAY <details> <summary> Items </summary> [{STRING\(marketing_permission_id), BOOLEAN\(enabled)}] </details> | The marketing permissions for the subscriber. | false |
| ip_signup | Ip Signup | STRING | IP address the subscriber signed up from. | false |
| timestamp_signup | Timestamp Signup | STRING | The date and time the subscriber signed up for the list in ISO 8601 format. | false |
| ip_opt | Ip Opt | STRING | The IP address the subscriber used to confirm their opt-in status. | false |
| timestamp_opt | Timestamp Opt | STRING | The date and time the subscriber confirmed their opt-in status in ISO 8601 format. | false |
| tags | Tags | ARRAY <details> <summary> Items </summary> [STRING] </details> | The tags that are associated with a member. | false |


#### Output


___Sample Output:___

```{email_type=string, marketing_permissions=[{text=string, enabled=true, marketing_permission_id=string}], tags=[{name=string, id=0}], ip_signup=string, location={region=string, longitude=0, gmtoff=0, country_code=string, timezone=string, dstoff=0, latitude=0}, ip_opt=string, timestamp_opt=2019-08-24T14:15:22, unsubscribe_reason=string, status=subscribed, email_address=string, last_note={note=string, created_at=2019-08-24T14:15:22, note_id=0, created_by=string}, contact_id=string, stats={ecommerce_data={total_revenue=0, currency_code=USD, number_of_orders=0}, avg_open_rate=0, avg_click_rate=0}, merge_fields={property2=, property1=}, full_name=string, list_id=string, tags_count=0, unique_email_id=string, email_client=string, consents_to_one_to_one_messaging=true, source=string, last_changed=2019-08-24T14:15:22, vip=true, member_rating=0, web_id=0, _links=[{targetSchema=string, method=GET, rel=string, href=string, schema=string}], id=string, timestamp_signup=2019-08-24T14:15:22, interests={property2=true, property1=true}, language=string}```



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| email_address | STRING |
| unique_email_id | STRING |
| contact_id | STRING |
| full_name | STRING |
| web_id | STRING |
| email_type | STRING |
| status | STRING <details> <summary> Options </summary> subscribed, unsubscribed, cleaned, pending, transactional </details> |
| unsubscribe_reason | STRING |
| consents_to_one_to_one_messaging | BOOLEAN <details> <summary> Options </summary> true, false </details> |
| merge_fields | OBJECT <details> <summary> Properties </summary> {} </details> |
| interests | OBJECT <details> <summary> Properties </summary> {} </details> |
| stats | OBJECT <details> <summary> Properties </summary> {NUMBER\(avg_open_rate), NUMBER\(avg_click_rate), {NUMBER\(total_revenue), NUMBER\(number_of_orders), STRING\(currency_code)}\(ecommerce_data)} </details> |
| ip_signup | STRING |
| timestamp_signup | STRING |
| ip_opt | STRING |
| timestamp_opt | STRING |
| member_rating | INTEGER |
| last_changed | STRING |
| language | STRING |
| vip | BOOLEAN <details> <summary> Options </summary> true, false </details> |
| email_client | STRING |
| location | OBJECT <details> <summary> Properties </summary> {NUMBER\(latitude), NUMBER\(longitude), INTEGER\(gmtoff), INTEGER\(dstoff), STRING\(country_code), STRING\(timezone), STRING\(region)} </details> |
| marketing_permissions | ARRAY <details> <summary> Items </summary> [{STRING\(marketing_permission_id), STRING\(text), BOOLEAN\(enabled)}] </details> |
| last_note | OBJECT <details> <summary> Properties </summary> {INTEGER\(note_id), STRING\(created_at), STRING\(created_by), STRING\(note)} </details> |
| source | STRING |
| tags_count | INTEGER |
| tags | OBJECT <details> <summary> Properties </summary> {INTEGER\(id), STRING\(name)} </details> |
| list_id | STRING |
| _links | ARRAY <details> <summary> Items </summary> [{STRING\(rel), STRING\(href), STRING\(method), STRING\(targetSchema), STRING\(schema)}] </details> |




#### JSON Example
```json
{
  "label" : "Add Member to List",
  "name" : "addMemberToList",
  "parameters" : {
    "listId" : "",
    "skip_merge_validation" : false,
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

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| listId | List Id | STRING | The list id of intended audience to which you would like to add the contact. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| data | OBJECT <details> <summary> Properties </summary> {STRING\(email), STRING\(email_type), STRING\(id), STRING\(ip_opt), STRING\(ip_signup), STRING\(list_id), {STRING\(EMAIL), STRING\(FNAME), STRING\(INTERESTS), STRING\(LNAME)}\(merges)} </details> |
| fired_at | DATE_TIME |
| type | STRING |




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

