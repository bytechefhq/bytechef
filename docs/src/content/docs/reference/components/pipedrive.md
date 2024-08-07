---
title: "Pipedrive"
description: "The first CRM designed by salespeople, for salespeople. Do more to grow your business."
---
## Reference
<hr />

The first CRM designed by salespeople, for salespeople. Do more to grow your business.


Categories: [CRM]


Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Value | STRING | TEXT  |  |
| Add to | STRING | TEXT  |  |



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


___Sample Output:___

```{
    "id": 8,
    "company_id": 22122,
    "user_id": 1234,
    "done": false,
    "type": "deadline",
    "reference_type": "scheduler-service",
    "reference_id": 7,
    "conference_meeting_client": "871b8bc88d3a1202",
    "conference_meeting_url": "https://pipedrive.zoom.us/link",
    "conference_meeting_id": "01758746701",
    "due_date": "2020-06-09",
    "due_time": "10:00",
    "duration": "01:00",
    "busy_flag": true,
    "add_time": "2020-06-08 12:37:56",
    "marked_as_done_time": "2020-08-08 08:08:38",
    "last_notification_time": "2020-08-08 12:37:56",
    "last_notification_user_id": 7655,
    "notification_language_id": 1,
    "subject": "Deadline",
    "public_description": "This is a description",
    "calendar_sync_include_context": "",
    "location": "Mustamäe tee 3, Tallinn, Estonia",
    "org_id": 5,
    "person_id": 1101,
    "deal_id": 300,
    "lead_id": "46c3b0e1-db35-59ca-1828-4817378dff71",
    "project_id": null,
    "active_flag": true,
    "update_time": "2020-08-08 12:37:56",
    "update_user_id": 5596,
    "gcal_event_id": "",
    "google_calendar_id": "",
    "google_calendar_etag": "",
    "source_timezone": "",
    "rec_rule": "RRULE:FREQ=WEEKLY;BYDAY=WE",
    "rec_rule_extension": "",
    "rec_master_activity_id": 1,
    "series": [],
    "note": "A note for the activity",
    "created_by_user_id": 1234,
    "location_subpremise": "",
    "location_street_number": "3",
    "location_route": "Mustamäe tee",
    "location_sublocality": "Kristiine",
    "location_locality": "Tallinn",
    "location_admin_area_level_1": "Harju maakond",
    "location_admin_area_level_2": "",
    "location_country": "Estonia",
    "location_postal_code": "10616",
    "location_formatted_address": "Mustamäe tee 3, 10616 Tallinn, Estonia",
    "attendees":
    [
        {
            "email_address": "attendee@pipedrivemail.com",
            "is_organizer": 0,
            "name": "Attendee",
            "person_id": 25312,
            "status": "noreply",
            "user_id": null
        }
    ],
    "participants":
    [
        {
            "person_id": 17985,
            "primary_flag": false
        },
        {
            "person_id": 1101,
            "primary_flag": true
        }
    ],
    "org_name": "Organization",
    "person_name": "Person",
    "deal_title": "Deal",
    "owner_name": "Creator",
    "person_dropbox_bcc": "company@pipedrivemail.com",
    "deal_dropbox_bcc": "company+deal300@pipedrivemail.com",
    "assigned_to_user_id": 1235,
    "file":
    {
        "id": "376892,",
        "clean_name": "Audio 10:55:07.m4a",
        "url": "https://pipedrive-files.s3-eu-west-1.amazonaws.com/Audio-recording.m4a"
    }
}
```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| TIME | TIME  |
| TIME | TIME  |
| BOOLEAN | SELECT  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| DATE_TIME | DATE_TIME  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| [] | ARRAY_BUILDER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(email_address), INTEGER(is_organizer), STRING(name), INTEGER(person_id), STRING(status), STRING(user_id)}] | ARRAY_BUILDER  |
| [{INTEGER(person_id), BOOLEAN(primary_flag)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| {STRING(id), STRING(clean_name), STRING(url)} | OBJECT_BUILDER  |






### New Deal
Trigger off whenever a new deal is added.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output


___Sample Output:___

```{
    "id": 1,
    "creator_user_id":
    {
        "id": 8877,
        "name": "Creator",
        "email": "john.doe@pipedrive.com",
        "has_pic": false,
        "pic_hash": null,
        "active_flag": true,
        "value": 8877
    },
    "user_id":
    {
        "id": 8877,
        "name": "Creator",
        "email": "john.doe@pipedrive.com",
        "has_pic": false,
        "pic_hash": null,
        "active_flag": true,
        "value": 8877
    },
    "person_id":
    {
        "active_flag": true,
        "name": "Person",
        "email":
        [
            {
                "label": "work",
                "value": "person@pipedrive.com",
                "primary": true
            }
        ],
        "phone":
        [
            {
                "label": "work",
                "value": "37244499911",
                "primary": true
            }
        ],
        "value": 1101
    },
    "org_id":
    {
        "name": "Organization",
        "people_count": 2,
        "owner_id": 8877,
        "address": "",
        "active_flag": true,
        "cc_email": "org@pipedrivemail.com",
        "value": 5
    },
    "stage_id": 2,
    "title": "Deal One",
    "value": 5000,
    "currency": "EUR",
    "add_time": "2019-05-29 04:21:51",
    "update_time": "2019-11-28 16:19:50",
    "stage_change_time": "2019-11-28 15:41:22",
    "active": true,
    "deleted": false,
    "status": "open",
    "probability": null,
    "next_activity_date": "2019-11-29",
    "next_activity_time": "11:30:00",
    "next_activity_id": 128,
    "last_activity_id": null,
    "last_activity_date": null,
    "lost_reason": null,
    "visible_to": "1",
    "close_time": null,
    "pipeline_id": 1,
    "won_time": "2019-11-27 11:40:36",
    "first_won_time": "2019-11-27 11:40:36",
    "lost_time": "",
    "products_count": 0,
    "files_count": 0,
    "notes_count": 2,
    "followers_count": 0,
    "email_messages_count": 4,
    "activities_count": 1,
    "done_activities_count": 0,
    "undone_activities_count": 1,
    "participants_count": 1,
    "expected_close_date": "2019-06-29",
    "last_incoming_mail_time": "2019-05-29 18:21:42",
    "last_outgoing_mail_time": "2019-05-30 03:45:35",
    "label": "11",
    "stage_order_nr": 2,
    "person_name": "Person",
    "org_name": "Organization",
    "next_activity_subject": "Call",
    "next_activity_type": "call",
    "next_activity_duration": "00:30:00",
    "next_activity_note": "Note content",
    "formatted_value": "€5,000",
    "weighted_value": 5000,
    "formatted_weighted_value": "€5,000",
    "weighted_value_currency": "EUR",
    "rotten_time": null,
    "owner_name": "Creator",
    "cc_email": "company+deal1@pipedrivemail.com",
    "org_hidden": false,
    "person_hidden": false
}
```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| {INTEGER(id), STRING(name), STRING(email), BOOLEAN(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| {INTEGER(id), STRING(name), STRING(email), BOOLEAN(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| {BOOLEAN(active_flag), STRING(name), [{STRING(label), STRING(value), BOOLEAN(primary)}](email), [{STRING(label), STRING(value), BOOLEAN(primary)}](phone), INTEGER(value)} | OBJECT_BUILDER  |
| {STRING(name), INTEGER(people_count), INTEGER(owner_id), STRING(address), BOOLEAN(active_flag), STRING(cc_email), INTEGER(value)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |






### New Organization
Trigger off whenever a new organization is added.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output


___Sample Output:___

```{
    "id": 1,
    "company_id": 77,
    "owner_id":
    {
        "id": 10,
        "name": "Will Smith",
        "email": "will.smith@pipedrive.com",
        "has_pic": 0,
        "pic_hash": "2611ace8ac6a3afe2f69ed56f9e08c6b",
        "active_flag": true,
        "value": 10
    },
    "name": "Bolt",
    "open_deals_count": 1,
    "related_open_deals_count": 2,
    "closed_deals_count": 3,
    "related_closed_deals_count": 1,
    "email_messages_count": 2,
    "people_count": 1,
    "activities_count": 2,
    "done_activities_count": 1,
    "undone_activities_count": 0,
    "files_count": 0,
    "notes_count": 0,
    "followers_count": 1,
    "won_deals_count": 0,
    "related_won_deals_count": 0,
    "lost_deals_count": 0,
    "related_lost_deals_count": 0,
    "active_flag": true,
    "picture_id":
    {
        "item_type": "person",
        "item_id": 25,
        "active_flag": true,
        "add_time": "2020-09-08 08:17:52",
        "update_time": "0000-00-00 00:00:00",
        "added_by_user_id": 967055,
        "pictures":
        {
            "128": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg",
            "512": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg"
        },
        "value": 101
    },
    "country_code": "USA",
    "first_char": "b",
    "update_time": "2020-09-08 12:14:11",
    "add_time": "2020-02-25 10:04:08",
    "visible_to": "3",
    "next_activity_date": "2019-11-29",
    "next_activity_time": "11:30:00",
    "next_activity_id": 128,
    "last_activity_id": 34,
    "last_activity_date": "2019-11-28",
    "label": 7,
    "address": "Mustamäe tee 3a, 10615 Tallinn",
    "address_subpremise": "",
    "address_street_number": "3a",
    "address_route": "Mustamäe tee",
    "address_sublocality": "Kristiine",
    "address_locality": "Tallinn",
    "address_admin_area_level_1": "Harju maakond",
    "address_admin_area_level_2": "",
    "address_country": "Estonia",
    "address_postal_code": "10616",
    "address_formatted_address": "Mustamäe tee 3a, 10616 Tallinn, Estonia",
    "owner_name": "John Doe",
    "cc_email": "org@pipedrivemail.com"
}
```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| {INTEGER(id), STRING(name), STRING(email), INTEGER(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| {STRING(item_type), INTEGER(item_id), BOOLEAN(active_flag), DATE_TIME(add_time), DATE_TIME(update_time), INTEGER(added_by_user_id), {STRING(128), STRING(512)}(pictures), INTEGER(value)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| DATE | DATE  |
| TIME | TIME  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### New Person
Trigger off whenever a new person is added.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output


___Sample Output:___

```{
    "id": 1,
    "company_id": 12,
    "owner_id": {
      "id": 123,
      "name": "Jane Doe",
      "email": "jane@pipedrive.com",
      "has_pic": 1,
      "pic_hash": "2611ace8ac6a3afe2f69ed56f9e08c6b",
      "active_flag": true,
      "value": 123
    },
    "org_id": {
      "name": "Org Name",
      "people_count": 1,
      "owner_id": 123,
      "address": "Mustamäe tee 3a, 10615 Tallinn",
      "active_flag": true,
      "cc_email": "org@pipedrivemail.com",
      "value": 1234
    },
    "name": "Will Smith",
    "first_name": "Will",
    "last_name": "Smith",
    "open_deals_count": 2,
    "related_open_deals_count": 2,
    "closed_deals_count": 3,
    "related_closed_deals_count": 3,
    "participant_open_deals_count": 1,
    "participant_closed_deals_count": 1,
    "email_messages_count": 1,
    "activities_count": 1,
    "done_activities_count": 1,
    "undone_activities_count": 2,
    "files_count": 2,
    "notes_count": 2,
    "followers_count": 3,
    "won_deals_count": 3,
    "related_won_deals_count": 3,
    "lost_deals_count": 1,
    "related_lost_deals_count": 1,
    "active_flag": true,
    "phone": [
      {
        "value": "12345",
        "primary": true,
        "label": "work"
      }
    ],
    "email": [
      {
        "value": "12345@email.com",
        "primary": true,
        "label": "work"
      }
    ],
    "primary_email": "12345@email.com",
    "first_char": "w",
    "update_time": "2020-05-08 05:30:20",
    "add_time": "2017-10-18 13:23:07",
    "visible_to": "3",
    "marketing_status": "no_consent",
    "picture_id": {
      "item_type": "person",
      "item_id": 25,
      "active_flag": true,
      "add_time": "2020-09-08 08:17:52",
      "update_time": "0000-00-00 00:00:00",
      "added_by_user_id": 967055,
      "pictures": {
        "128": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg",
        "512": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg"
      },
      "value": 4
    },
    "next_activity_date": "2019-11-29",
    "next_activity_time": "11:30:00",
    "next_activity_id": 128,
    "last_activity_id": 34,
    "last_activity_date": "2019-11-28",
    "last_incoming_mail_time": "2019-05-29 18:21:42",
    "last_outgoing_mail_time": "2019-05-30 03:45:35",
    "label": 1,
    "org_name": "Organization name",
    "owner_name": "Jane Doe",
    "cc_email": "org@pipedrivemail.com"
  }
```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| {INTEGER(id), STRING(name), STRING(email), INTEGER(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| {INTEGER(id), INTEGER(people_count), INTEGER(owner_id), STRING(address), BOOLEAN(active_flag), STRING(cc_email), INTEGER(value)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| [{STRING(label), STRING(value), BOOLEAN(primary)}] | ARRAY_BUILDER  |
| [{STRING(label), STRING(value), BOOLEAN(primary)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(item_type), INTEGER(item_id), BOOLEAN(active_flag), DATE_TIME(add_time), DATE_TIME(update_time), INTEGER(added_by_user_id), {STRING(128), STRING(512)}(pictures), INTEGER(value)} | OBJECT_BUILDER  |
| DATE | DATE  |
| TIME | TIME  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Updated Deal
Trigger off whenever an existing deal is updated.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output


___Sample Output:___

```{
    "id": 1,
    "creator_user_id":
    {
        "id": 8877,
        "name": "Creator",
        "email": "john.doe@pipedrive.com",
        "has_pic": false,
        "pic_hash": null,
        "active_flag": true,
        "value": 8877
    },
    "user_id":
    {
        "id": 8877,
        "name": "Creator",
        "email": "john.doe@pipedrive.com",
        "has_pic": false,
        "pic_hash": null,
        "active_flag": true,
        "value": 8877
    },
    "person_id":
    {
        "active_flag": true,
        "name": "Person",
        "email":
        [
            {
                "label": "work",
                "value": "person@pipedrive.com",
                "primary": true
            }
        ],
        "phone":
        [
            {
                "label": "work",
                "value": "37244499911",
                "primary": true
            }
        ],
        "value": 1101
    },
    "org_id":
    {
        "name": "Organization",
        "people_count": 2,
        "owner_id": 8877,
        "address": "",
        "active_flag": true,
        "cc_email": "org@pipedrivemail.com",
        "value": 5
    },
    "stage_id": 2,
    "title": "Deal One",
    "value": 5000,
    "currency": "EUR",
    "add_time": "2019-05-29 04:21:51",
    "update_time": "2019-11-28 16:19:50",
    "stage_change_time": "2019-11-28 15:41:22",
    "active": true,
    "deleted": false,
    "status": "open",
    "probability": null,
    "next_activity_date": "2019-11-29",
    "next_activity_time": "11:30:00",
    "next_activity_id": 128,
    "last_activity_id": null,
    "last_activity_date": null,
    "lost_reason": null,
    "visible_to": "1",
    "close_time": null,
    "pipeline_id": 1,
    "won_time": "2019-11-27 11:40:36",
    "first_won_time": "2019-11-27 11:40:36",
    "lost_time": "",
    "products_count": 0,
    "files_count": 0,
    "notes_count": 2,
    "followers_count": 0,
    "email_messages_count": 4,
    "activities_count": 1,
    "done_activities_count": 0,
    "undone_activities_count": 1,
    "participants_count": 1,
    "expected_close_date": "2019-06-29",
    "last_incoming_mail_time": "2019-05-29 18:21:42",
    "last_outgoing_mail_time": "2019-05-30 03:45:35",
    "label": "11",
    "stage_order_nr": 2,
    "person_name": "Person",
    "org_name": "Organization",
    "next_activity_subject": "Call",
    "next_activity_type": "call",
    "next_activity_duration": "00:30:00",
    "next_activity_note": "Note content",
    "formatted_value": "€5,000",
    "weighted_value": 5000,
    "formatted_weighted_value": "€5,000",
    "weighted_value_currency": "EUR",
    "rotten_time": null,
    "owner_name": "Creator",
    "cc_email": "company+deal1@pipedrivemail.com",
    "org_hidden": false,
    "person_hidden": false
}
```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| {INTEGER(id), STRING(name), STRING(email), BOOLEAN(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| {INTEGER(id), STRING(name), STRING(email), BOOLEAN(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| {BOOLEAN(active_flag), STRING(name), [{STRING(label), STRING(value), BOOLEAN(primary)}](email), [{STRING(label), STRING(value), BOOLEAN(primary)}](phone), INTEGER(value)} | OBJECT_BUILDER  |
| {STRING(name), INTEGER(people_count), INTEGER(owner_id), STRING(address), BOOLEAN(active_flag), STRING(cc_email), INTEGER(value)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |






### Updated Organization
Trigger off whenever an existing organization is updated.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output


___Sample Output:___

```{
    "id": 1,
    "company_id": 77,
    "owner_id":
    {
        "id": 10,
        "name": "Will Smith",
        "email": "will.smith@pipedrive.com",
        "has_pic": 0,
        "pic_hash": "2611ace8ac6a3afe2f69ed56f9e08c6b",
        "active_flag": true,
        "value": 10
    },
    "name": "Bolt",
    "open_deals_count": 1,
    "related_open_deals_count": 2,
    "closed_deals_count": 3,
    "related_closed_deals_count": 1,
    "email_messages_count": 2,
    "people_count": 1,
    "activities_count": 2,
    "done_activities_count": 1,
    "undone_activities_count": 0,
    "files_count": 0,
    "notes_count": 0,
    "followers_count": 1,
    "won_deals_count": 0,
    "related_won_deals_count": 0,
    "lost_deals_count": 0,
    "related_lost_deals_count": 0,
    "active_flag": true,
    "picture_id":
    {
        "item_type": "person",
        "item_id": 25,
        "active_flag": true,
        "add_time": "2020-09-08 08:17:52",
        "update_time": "0000-00-00 00:00:00",
        "added_by_user_id": 967055,
        "pictures":
        {
            "128": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg",
            "512": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg"
        },
        "value": 101
    },
    "country_code": "USA",
    "first_char": "b",
    "update_time": "2020-09-08 12:14:11",
    "add_time": "2020-02-25 10:04:08",
    "visible_to": "3",
    "next_activity_date": "2019-11-29",
    "next_activity_time": "11:30:00",
    "next_activity_id": 128,
    "last_activity_id": 34,
    "last_activity_date": "2019-11-28",
    "label": 7,
    "address": "Mustamäe tee 3a, 10615 Tallinn",
    "address_subpremise": "",
    "address_street_number": "3a",
    "address_route": "Mustamäe tee",
    "address_sublocality": "Kristiine",
    "address_locality": "Tallinn",
    "address_admin_area_level_1": "Harju maakond",
    "address_admin_area_level_2": "",
    "address_country": "Estonia",
    "address_postal_code": "10616",
    "address_formatted_address": "Mustamäe tee 3a, 10616 Tallinn, Estonia",
    "owner_name": "John Doe",
    "cc_email": "org@pipedrivemail.com"
}
```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| {INTEGER(id), STRING(name), STRING(email), INTEGER(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| {STRING(item_type), INTEGER(item_id), BOOLEAN(active_flag), DATE_TIME(add_time), DATE_TIME(update_time), INTEGER(added_by_user_id), {STRING(128), STRING(512)}(pictures), INTEGER(value)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| DATE | DATE  |
| TIME | TIME  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Updated Person
Trigger off whenever an existing person is updated.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null


### Output


___Sample Output:___

```{
    "id": 1,
    "company_id": 12,
    "owner_id": {
      "id": 123,
      "name": "Jane Doe",
      "email": "jane@pipedrive.com",
      "has_pic": 1,
      "pic_hash": "2611ace8ac6a3afe2f69ed56f9e08c6b",
      "active_flag": true,
      "value": 123
    },
    "org_id": {
      "name": "Org Name",
      "people_count": 1,
      "owner_id": 123,
      "address": "Mustamäe tee 3a, 10615 Tallinn",
      "active_flag": true,
      "cc_email": "org@pipedrivemail.com",
      "value": 1234
    },
    "name": "Will Smith",
    "first_name": "Will",
    "last_name": "Smith",
    "open_deals_count": 2,
    "related_open_deals_count": 2,
    "closed_deals_count": 3,
    "related_closed_deals_count": 3,
    "participant_open_deals_count": 1,
    "participant_closed_deals_count": 1,
    "email_messages_count": 1,
    "activities_count": 1,
    "done_activities_count": 1,
    "undone_activities_count": 2,
    "files_count": 2,
    "notes_count": 2,
    "followers_count": 3,
    "won_deals_count": 3,
    "related_won_deals_count": 3,
    "lost_deals_count": 1,
    "related_lost_deals_count": 1,
    "active_flag": true,
    "phone": [
      {
        "value": "12345",
        "primary": true,
        "label": "work"
      }
    ],
    "email": [
      {
        "value": "12345@email.com",
        "primary": true,
        "label": "work"
      }
    ],
    "primary_email": "12345@email.com",
    "first_char": "w",
    "update_time": "2020-05-08 05:30:20",
    "add_time": "2017-10-18 13:23:07",
    "visible_to": "3",
    "marketing_status": "no_consent",
    "picture_id": {
      "item_type": "person",
      "item_id": 25,
      "active_flag": true,
      "add_time": "2020-09-08 08:17:52",
      "update_time": "0000-00-00 00:00:00",
      "added_by_user_id": 967055,
      "pictures": {
        "128": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg",
        "512": "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg"
      },
      "value": 4
    },
    "next_activity_date": "2019-11-29",
    "next_activity_time": "11:30:00",
    "next_activity_id": 128,
    "last_activity_id": 34,
    "last_activity_date": "2019-11-28",
    "last_incoming_mail_time": "2019-05-29 18:21:42",
    "last_outgoing_mail_time": "2019-05-30 03:45:35",
    "label": 1,
    "org_name": "Organization name",
    "owner_name": "Jane Doe",
    "cc_email": "org@pipedrivemail.com"
  }
```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| {INTEGER(id), STRING(name), STRING(email), INTEGER(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| {INTEGER(id), INTEGER(people_count), INTEGER(owner_id), STRING(address), BOOLEAN(active_flag), STRING(cc_email), INTEGER(value)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| [{STRING(label), STRING(value), BOOLEAN(primary)}] | ARRAY_BUILDER  |
| [{STRING(label), STRING(value), BOOLEAN(primary)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(item_type), INTEGER(item_id), BOOLEAN(active_flag), DATE_TIME(add_time), DATE_TIME(update_time), INTEGER(added_by_user_id), {STRING(128), STRING(512)}(pictures), INTEGER(value)} | OBJECT_BUILDER  |
| DATE | DATE  |
| TIME | TIME  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| DATE | DATE  |
| DATE_TIME | DATE_TIME  |
| DATE_TIME | DATE_TIME  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






<hr />



## Actions


### Get all deals
Returns all deals. For more information, see the tutorial for <a href="https://pipedrive.readme.io/docs/getting-all-deals" target="_blank" rel="noopener noreferrer">getting all deals</a>.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| User Id | INTEGER | SELECT  |  If supplied, only deals matching the given user will be returned. However, `filter_id` and `owned_by_you` takes precedence over `user_id` when supplied.  |
| Filter Id | INTEGER | SELECT  |  The ID of the filter to use  |
| Stage Id | INTEGER | SELECT  |  If supplied, only deals within the given stage will be returned  |
| Status | STRING | SELECT  |  Only fetch deals with a specific status. If omitted, all not deleted deals are returned. If set to deleted, deals that have been deleted up to 30 days ago will be included.  |
| Start | INTEGER | INTEGER  |  Pagination start  |
| Limit | INTEGER | INTEGER  |  Items shown per page  |
| Sort | STRING | TEXT  |  The field names and sorting mode separated by a comma (`field_name_1 ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).  |
| Number Boolean | NUMBER | SELECT  |  When supplied, only deals owned by you are returned. However, `filter_id` takes precedence over `owned_by_you` when both are supplied.  |


### Output


___Sample Output:___

```{related_objects={stage={2={active_flag=true, pipeline_name=Pipeline, company_id=123, update_time=2015-12-08 13:54:06, rotten_flag=false, id=2, rotten_days=, name=Stage Name, pipeline_deal_probability=true, add_time=2015-12-08 13:54:06, pipeline_id=1, deal_probability=100, order_nr=1}}, organization={5={owner_id=8877, active_flag=true, name=Organization, people_count=2, address=Mustamäe tee 3a, 10615 Tallinn, id=5, cc_email=org@pipedrivemail.com}}, person={1101={name=Person, owner_id=8877, phone=[{value=3.421787767E9, primary=true, label=work}], id=1101, active_flag=true, email=[{value=person@pipedrive.com, primary=true, label=work}]}}, user={8877={name=Creator, has_pic=false, pic_hash=, id=8877, active_flag=true, email=john.doe@pipedrive.com}}, pipeline={1={update_time=2015-12-08 10:00:24, deal_probability=true, add_time=2015-12-08 10:00:24, name=Pipeline, id=1, url_title=Pipeline, active=true, order_nr=0}}}, additional_data={pagination={more_items_in_collection=false, next_start=1, limit=100, start=0}}, success=true, data=[{org_name=Organization, title=Deal One, last_outgoing_mail_time=2019-05-30 03:45:35, pipeline_id=1, last_activity_date=, org_id={owner_id=8877, active_flag=true, name=Organization, people_count=2, address=Mustamäe tee 3a, 10615 Tallinn, value=5, cc_email=org@pipedrivemail.com}, cc_email=company+deal1@pipedrivemail.com, probability=, add_time=2019-05-29 04:21:51, formatted_weighted_value=€5,000, visible_to=1.0, stage_change_time=2019-11-28 15:41:22, next_activity_type=call, org_hidden=false, person_name=Person, weighted_value_currency=EUR, lost_time=2019-11-27 11:40:36, weighted_value=5000, next_activity_note=Note content, done_activities_count=0, creator_user_id={pic_hash=, email=john.doe@pipedrive.com, name=Creator, has_pic=false, id=8877, value=8877, active_flag=true}, undone_activities_count=1, currency=EUR, activities_count=1, lost_reason=, next_activity_subject=Call, stage_id=2, expected_close_date=2019-06-29, participants_count=1, email_messages_count=4, files_count=0, products_count=0, value=5000, update_time=2019-11-28 16:19:50, person_hidden=false, next_activity_date=2019-11-29, active=true, followers_count=0, person_id={value=1101, email=[{value=person@pipedrive.com, primary=true, label=work}], active_flag=true, phone=[{value=3.7244499911E10, primary=true, label=work}], name=Person}, next_activity_id=128, rotten_time=, label=11, deleted=false, formatted_value=€5,000, next_activity_duration=00:30:00, last_activity_id=, close_time=, next_activity_time=11:30:00, won_time=2019-11-27 11:40:36, first_won_time=2019-11-27 11:40:36, status=open, id=1, user_id={pic_hash=, email=john.doe@pipedrive.com, name=Creator, has_pic=false, id=8877, value=8877, active_flag=true}, notes_count=2, stage_order_nr=2, owner_name=Creator, last_incoming_mail_time=2019-05-29 18:21:42}]}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| [{INTEGER(email_messages_count), STRING(cc_email), INTEGER(products_count), STRING(next_activity_date), STRING(next_activity_type), STRING(next_activity_duration), INTEGER(id), STRING(name), BOOLEAN(active_flag), [{STRING(label), STRING(value), BOOLEAN(primary)}](phone), INTEGER(value), [{STRING(label), STRING(value), BOOLEAN(primary)}](email), INTEGER(owner_id), {INTEGER(id), STRING(name), STRING(email), BOOLEAN(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)}(creator_user_id), DATE(expected_close_date), INTEGER(participants_count), STRING(owner_name), INTEGER(stage_id), NUMBER(probability), INTEGER(undone_activities_count), BOOLEAN(active), STRING(last_activity_date), STRING(person_name), STRING(close_time), INTEGER(next_activity_id), STRING(weighted_value_currency), BOOLEAN(org_hidden), INTEGER(stage_order_nr), STRING(next_activity_subject), STRING(rotten_time), STRING(name), BOOLEAN(has_pic), BOOLEAN(active_flag), INTEGER(id), INTEGER(value), STRING(email), STRING(pic_hash), STRING(visible_to), STRING(address), INTEGER(owner_id), STRING(cc_email), STRING(name), BOOLEAN(active_flag), INTEGER(people_count), INTEGER(value), INTEGER(notes_count), STRING(next_activity_time), STRING(formatted_value), STRING(status), STRING(formatted_weighted_value), STRING(first_won_time), STRING(last_outgoing_mail_time), STRING(title), INTEGER(last_activity_id), STRING(update_time), INTEGER(activities_count), INTEGER(pipeline_id), STRING(lost_time), STRING(currency), NUMBER(weighted_value), STRING(org_name), NUMBER(value), STRING(next_activity_note), BOOLEAN(person_hidden), INTEGER(files_count), STRING(last_incoming_mail_time), INTEGER(label), STRING(lost_reason), BOOLEAN(deleted), STRING(won_time), INTEGER(followers_count), STRING(stage_change_time), STRING(add_time), INTEGER(done_activities_count)}] | ARRAY_BUILDER  |
| {INTEGER(start), INTEGER(limit), BOOLEAN(more_items_in_collection)} | OBJECT_BUILDER  |
| {{}(user), {}(organization), {}(person)} | OBJECT_BUILDER  |





### Add a deal
Adds a new deal. Note that you can supply additional custom fields along with the request that are not described here. These custom fields are different for each Pipedrive account and can be recognized by long hashes as keys. To determine which custom fields exists, fetch the dealFields and look for `key` values. For more information, see the tutorial for <a href="https://pipedrive.readme.io/docs/creating-a-deal" target="_blank" rel="noopener noreferrer">adding a deal</a>.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Add Deal Request | {DATE(expected_close_date), INTEGER(stage_id), NUMBER(probability), STRING(title), STRING(lost_reason), INTEGER(user_id), STRING(visible_to), INTEGER(org_id), INTEGER(pipeline_id), STRING(currency), STRING(value), STRING(add_time), INTEGER(person_id), STRING(status)} | OBJECT_BUILDER  |  |


### Output


___Sample Output:___

```{success=true, related_objects={stage={2={active_flag=true, pipeline_name=Pipeline, company_id=123, update_time=2015-12-08 13:54:06, rotten_flag=false, id=2, rotten_days=, name=Stage Name, pipeline_deal_probability=true, add_time=2015-12-08 13:54:06, pipeline_id=1, deal_probability=100, order_nr=1}}, organization={2={owner_id=8877, active_flag=true, name=Organization, people_count=2, address=Mustamäe tee 3a, 10615 Tallinn, id=2, cc_email=org@pipedrivemail.com}}, person={1101={name=Person, owner_id=8877, phone=[{value=3.421787767E9, primary=true, label=work}], id=1101, active_flag=true, email=[{value=person@pipedrive.com, primary=true, label=work}]}}, user={8877={name=Creator, has_pic=false, pic_hash=, id=8877, active_flag=true, email=john.doe@pipedrive.com}}, pipeline={1={update_time=2015-12-08 10:00:24, deal_probability=true, add_time=2015-12-08 10:00:24, name=Pipeline, id=1, url_title=Pipeline, active=true, order_nr=0}}}, data={org_name=Organization, title=Deal One, last_outgoing_mail_time=2019-05-30 03:45:35, pipeline_id=1, last_activity_date=, org_id={owner_id=8877, active_flag=true, name=Organization, people_count=2, address=, value=5, cc_email=org@pipedrivemail.com}, cc_email=company+deal1@pipedrivemail.com, probability=, add_time=2019-05-29 04:21:51, formatted_weighted_value=€5,000, visible_to=1.0, stage_change_time=2019-11-28 15:41:22, next_activity_type=call, org_hidden=false, person_name=Person, weighted_value_currency=EUR, lost_time=, weighted_value=5000, next_activity_note=Note content, done_activities_count=0, creator_user_id={pic_hash=, email=john.doe@pipedrive.com, name=Creator, has_pic=false, id=8877, value=8877, active_flag=true}, undone_activities_count=1, currency=EUR, activities_count=1, lost_reason=, next_activity_subject=Call, stage_id=2, expected_close_date=2019-06-29, participants_count=1, email_messages_count=4, files_count=0, products_count=0, value=5000, update_time=2019-11-28 16:19:50, person_hidden=false, next_activity_date=2019-11-29, active=true, followers_count=0, person_id={value=1101, email=[{value=person@pipedrive.com, primary=true, label=work}], active_flag=true, phone=[{value=3.7244499911E10, primary=true, label=work}], name=Person}, next_activity_id=128, rotten_time=, label=11, deleted=false, formatted_value=€5,000, next_activity_duration=00:30:00, last_activity_id=, close_time=, next_activity_time=11:30:00, won_time=2019-11-27 11:40:36, first_won_time=2019-11-27 11:40:36, status=open, id=1, user_id={pic_hash=, email=john.doe@pipedrive.com, name=Creator, has_pic=false, id=8877, value=8877, active_flag=true}, notes_count=2, stage_order_nr=2, owner_name=Creator, last_incoming_mail_time=2019-05-29 18:21:42}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| [{STRING(label), STRING(value), BOOLEAN(primary)}] | ARRAY_BUILDER  |
| INTEGER | INTEGER  |
| [{STRING(label), STRING(value), BOOLEAN(primary)}] | ARRAY_BUILDER  |
| INTEGER | INTEGER  |
| {INTEGER(id), STRING(name), STRING(email), BOOLEAN(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| DATE | DATE  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| NUMBER | NUMBER  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| {{}(user), {}(organization), {}(person)} | OBJECT_BUILDER  |





### Search deals
Searches all deals by title, notes and/or custom fields. This endpoint is a wrapper of <a href="https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem">/v1/itemSearch</a> with a narrower OAuth scope. Found deals can be filtered by the person ID and the organization ID.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |
| Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |
| Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |
| Person Id | INTEGER | SELECT  |  Will filter deals by the provided person ID. The upper limit of found deals associated with the person is 2000.  |
| Organization Id | INTEGER | INTEGER  |  Will filter deals by the provided organization ID. The upper limit of found deals associated with the organization is 2000.  |
| Status | STRING | SELECT  |  Will filter deals by the provided specific status. open = Open, won = Won, lost = Lost. The upper limit of found deals associated with the status is 2000.  |
| Include Fields | STRING | SELECT  |  Supports including optional fields in the results which are not provided by default  |
| Start | INTEGER | INTEGER  |  Pagination start. Note that the pagination is based on main results and does not include related items when using `search_for_related_items` parameter.  |
| Limit | INTEGER | INTEGER  |  Items shown per page  |


### Output


___Sample Output:___

```{success=true, additional_data={type=object, description=The additional data of the list, properties={start={description=Pagination start, type=integer}, more_items_in_collection={description=If there are more list items in the collection than displayed or not, type=boolean}, limit={description=Items shown per page, type=integer}}}, data={items=[{result_score=1.22, item={notes=[], currency=USD, organization=, value=100, stage={name=Lead In, id=1}, id=1, owner={id=1}, visible_to=3, person={name=Jane Doe, id=1}, status=open, title=Jane Doe deal, type=deal, custom_fields=[]}}]}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{NUMBER(result_score), {INTEGER(id), STRING(type), STRING(title), INTEGER(value), STRING(currency), STRING(status), INTEGER(visible_to), {INTEGER(id)}(owner), {INTEGER(id), STRING(name)}(stage), {INTEGER(id), STRING(name)}(person), {INTEGER(id), STRING(name)}(organization), [STRING](custom_fields), [STRING](notes)}(item)}](items)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| {{INTEGER(start), INTEGER(limit), BOOLEAN(more_items_in_collection), INTEGER(next_start)}(pagination)} | OBJECT_BUILDER  |





### Delete a deal
Marks a deal as deleted. After 30 days, the deal will be permanently deleted.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Id | INTEGER | INTEGER  |  The ID of the deal  |


### Output


___Sample Output:___

```{success=true, data={id=123}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| {INTEGER(id)} | OBJECT_BUILDER  |





### Get details of a deal
Returns the details of a specific deal. Note that this also returns some additional fields which are not present when asking for all deals – such as deal age and stay in pipeline stages. Also note that custom fields appear as long hashes in the resulting data. These hashes can be mapped against the `key` value of dealFields. For more information, see the tutorial for <a href="https://pipedrive.readme.io/docs/getting-details-of-a-deal" target="_blank" rel="noopener noreferrer">getting details of a deal</a>.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Id | INTEGER | INTEGER  |  The ID of the deal  |


### Output


___Sample Output:___

```{related_objects={stage={2={active_flag=true, pipeline_name=Pipeline, company_id=123, update_time=2015-12-08 13:54:06, rotten_flag=false, id=2, rotten_days=, name=Stage Name, pipeline_deal_probability=true, add_time=2015-12-08 13:54:06, pipeline_id=1, deal_probability=100, order_nr=1}}, organization={2={owner_id=8877, active_flag=true, name=Organization, people_count=2, address=Mustamäe tee 3a, 10615 Tallinn, id=2, cc_email=org@pipedrivemail.com}}, person={1101={name=Person, owner_id=8877, phone=[{value=3.421787767E9, primary=true, label=work}], id=1101, active_flag=true, email=[{value=person@pipedrive.com, primary=true, label=work}]}}, user={8877={name=Creator, has_pic=false, pic_hash=, id=8877, active_flag=true, email=john.doe@pipedrive.com}}, pipeline={1={update_time=2015-12-08 10:00:24, deal_probability=true, add_time=2015-12-08 10:00:24, name=Pipeline, id=1, url_title=Pipeline, active=true, order_nr=0}}}, additional_data={dropbox_email=company+deal1@pipedrivemail.com}, success=true, data={activities_count=1, add_time=2019-05-29 04:21:51, formatted_weighted_value=€5,000, formatted_value=€5,000, stage_order_nr=2, lost_reason=, last_activity_date=, last_activity_id=, update_time=2019-11-28 16:19:50, close_time=, cc_email=company+deal1@pipedrivemail.com, status=open, average_time_to_won={s=49, d=0, m=0, y=0, total_seconds=1249, i=20, h=0}, done_activities_count=0, won_time=2019-11-27 11:40:36, stage_id=2, last_activity=, next_activity_time=11:30:00, next_activity=, visible_to=1.0, email_messages_count=4, products_count=0, age={s=26, d=14, m=6, y=0, total_seconds=17139446, i=57, h=8}, average_stage_progress=4.99, label=11, deleted=false, weighted_value=5000, next_activity_note=Note content, stay_in_pipeline_stages={order_of_stages=[1, 2, 3, 4, 5], times_in_stages={4=3315, 3=4368, 2=1288449, 1=15721267, 5=26460}}, active=true, person_hidden=false, weighted_value_currency=EUR, next_activity_type=call, followers_count=0, pipeline_id=1, expected_close_date=2019-06-29, next_activity_date=2019-11-29, org_name=Organization, org_hidden=false, next_activity_subject=Call, person_name=Person, last_incoming_mail_time=2019-05-29 18:21:42, first_won_time=2019-11-27 11:40:36, id=1, title=Deal One, undone_activities_count=1, rotten_time=, probability=, currency=EUR, participants_count=1, lost_time=, person_id={value=1101, email=[{value=person@pipedrive.com, primary=true, label=work}], active_flag=true, phone=[{value=3.7244499911E10, primary=true, label=work}], name=Person}, creator_user_id={pic_hash=, email=john.doe@pipedrive.com, name=Creator, has_pic=false, id=8877, value=8877, active_flag=true}, stage_change_time=2019-11-28 15:41:22, org_id={owner_id=8877, active_flag=true, name=Organization, people_count=2, address=, value=5, cc_email=org@pipedrivemail.com}, user_id={pic_hash=, email=john.doe@pipedrive.com, name=Creator, has_pic=false, id=8877, value=8877, active_flag=true}, owner_name=Creator, files_count=0, next_activity_id=128, last_outgoing_mail_time=2019-05-30 03:45:35, notes_count=2, value=5000, next_activity_duration=00:30:00}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {{}(times_in_stages), [INTEGER](order_of_stages)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| [{STRING(label), STRING(value), BOOLEAN(primary)}] | ARRAY_BUILDER  |
| INTEGER | INTEGER  |
| [{STRING(label), STRING(value), BOOLEAN(primary)}] | ARRAY_BUILDER  |
| INTEGER | INTEGER  |
| {INTEGER(id), STRING(name), STRING(email), BOOLEAN(has_pic), STRING(pic_hash), BOOLEAN(active_flag), INTEGER(value)} | OBJECT_BUILDER  |
| DATE | DATE  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| NUMBER | NUMBER  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {INTEGER(y), INTEGER(m), INTEGER(d), INTEGER(h), INTEGER(i), INTEGER(s), INTEGER(total_seconds)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {} | OBJECT_BUILDER  |
| {} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| {INTEGER(y), INTEGER(m), INTEGER(d), INTEGER(h), INTEGER(i), INTEGER(s), INTEGER(total_seconds)} | OBJECT_BUILDER  |
| {STRING(dropbox_email)} | OBJECT_BUILDER  |
| {{}(user), {}(person), {}(organization)} | OBJECT_BUILDER  |





### Get all leads
Returns multiple leads. Leads are sorted by the time they were created, from oldest to newest. Pagination can be controlled using `limit` and `start` query parameters. If a lead contains custom fields, the fields' values will be included in the response in the same format as with the `Deals` endpoints. If a custom field's value hasn't been set for the lead, it won't appear in the response. Please note that leads do not have a separate set of custom fields, instead they inherit the custom fields' structure from deals.


#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Limit | INTEGER | INTEGER  |  For pagination, the limit of entries to be returned. If not provided, 100 items will be returned.  |
| Start | INTEGER | INTEGER  |  For pagination, the position that represents the first result for the page  |
| Archived Status | STRING | SELECT  |  Filtering based on the archived status of a lead. If not provided, `All` is used.  |
| Owner Id | INTEGER | SELECT  |  If supplied, only leads matching the given user will be returned. However, `filter_id` takes precedence over `owner_id` when supplied.  |
| Person Id | INTEGER | SELECT  |  If supplied, only leads matching the given person will be returned. However, `filter_id` takes precedence over `person_id` when supplied.  |
| Organization Id | INTEGER | INTEGER  |  If supplied, only leads matching the given organization will be returned. However, `filter_id` takes precedence over `organization_id` when supplied.  |
| Filter Id | INTEGER | SELECT  |  The ID of the filter to use  |
| Sort | STRING | SELECT  |  The field names and sorting mode separated by a comma (`field_name_1 ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).  |


### Output


___Sample Output:___

```{success=true, data=[{title=Jane Doe Lead, is_archived=false, organization_id=, add_time=2020-10-14T11:30:36, visible_to=3.0, was_seen=false, label_ids=[f08b42a0-4e75-11ea-9643-03698ef1cfd6, f08b42a1-4e75-11ea-9643-03698ef1cfd6], update_time=2020-10-14T11:30:36, expected_close_date=, owner_id=1, id=adf21080-0e10-11eb-879b-05d71fb426ec, source_name=API, cc_email=company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com, creator_id=1, person_id=1092, next_activity_id=1, value={amount=999, currency=USD}}]}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| [{STRING(id), STRING(title), INTEGER(owner_id), INTEGER(creator_id), [STRING](label_ids), INTEGER(person_id), INTEGER(organization_id), STRING(source_name), BOOLEAN(is_archived), BOOLEAN(was_seen), {NUMBER(amount), STRING(currency)}(value), DATE(expected_close_date), INTEGER(next_activity_id), DATE_TIME(add_time), DATE_TIME(update_time), STRING(visible_to), STRING(cc_email)}] | ARRAY_BUILDER  |
| {INTEGER(start), INTEGER(limit), BOOLEAN(more_items_in_collection)} | OBJECT_BUILDER  |





### Add a lead
Creates a lead. A lead always has to be linked to a person or an organization or both. All leads created through the Pipedrive API will have a lead source `API` assigned. Here's the tutorial for <a href="https://pipedrive.readme.io/docs/adding-a-lead" target="_blank" rel="noopener noreferrer">adding a lead</a>. If a lead contains custom fields, the fields' values will be included in the response in the same format as with the `Deals` endpoints. If a custom field's value hasn't been set for the lead, it won't appear in the response. Please note that leads do not have a separate set of custom fields, instead they inherit the custom fields' structure from deals. See an example given in the <a href="https://pipedrive.readme.io/docs/updating-custom-field-value" target="_blank" rel="noopener noreferrer">updating custom fields' values tutorial</a>.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Add Lead Request | {STRING(title), INTEGER(owner_id), [STRING](label_ids), INTEGER(person_id), INTEGER(organization_id), {NUMBER(amount), STRING(currency)}(value), DATE(expected_close_date), STRING(visible_to), BOOLEAN(was_seen)} | OBJECT_BUILDER  |  |


### Output


___Sample Output:___

```{success=true, data={title=Jane Doe Lead, is_archived=false, organization_id=, add_time=2020-10-14T11:30:36, visible_to=3.0, was_seen=false, label_ids=[f08b42a0-4e75-11ea-9643-03698ef1cfd6, f08b42a1-4e75-11ea-9643-03698ef1cfd6], update_time=2020-10-14T11:30:36, expected_close_date=, owner_id=1, id=adf21080-0e10-11eb-879b-05d71fb426ec, source_name=API, cc_email=company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com, creator_id=1, person_id=1092, next_activity_id=1, value={amount=999, currency=USD}}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| {STRING(id), STRING(title), INTEGER(owner_id), INTEGER(creator_id), [STRING](label_ids), INTEGER(person_id), INTEGER(organization_id), STRING(source_name), BOOLEAN(is_archived), BOOLEAN(was_seen), {NUMBER(amount), STRING(currency)}(value), DATE(expected_close_date), INTEGER(next_activity_id), DATE_TIME(add_time), DATE_TIME(update_time), STRING(visible_to), STRING(cc_email)} | OBJECT_BUILDER  |





### Delete a lead
Deletes a specific lead.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Id | STRING | TEXT  |  The ID of the lead  |


### Output


___Sample Output:___

```{success=true, data={id=adf21080-0e10-11eb-879b-05d71fb426ec}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| {STRING(id)} | OBJECT_BUILDER  |





### Get one lead
Returns details of a specific lead. If a lead contains custom fields, the fields' values will be included in the response in the same format as with the `Deals` endpoints. If a custom field's value hasn't been set for the lead, it won't appear in the response. Please note that leads do not have a separate set of custom fields, instead they inherit the custom fields’ structure from deals.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Id | STRING | TEXT  |  The ID of the lead  |


### Output


___Sample Output:___

```{success=true, data={title=Jane Doe Lead, is_archived=false, organization_id=, add_time=2020-10-14T11:30:36, visible_to=3.0, was_seen=false, label_ids=[f08b42a0-4e75-11ea-9643-03698ef1cfd6, f08b42a1-4e75-11ea-9643-03698ef1cfd6], update_time=2020-10-14T11:30:36, expected_close_date=, owner_id=1, id=adf21080-0e10-11eb-879b-05d71fb426ec, source_name=API, cc_email=company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com, creator_id=1, person_id=1092, next_activity_id=1, value={amount=999, currency=USD}}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| {STRING(id), STRING(title), INTEGER(owner_id), INTEGER(creator_id), [STRING](label_ids), INTEGER(person_id), INTEGER(organization_id), STRING(source_name), BOOLEAN(is_archived), BOOLEAN(was_seen), {NUMBER(amount), STRING(currency)}(value), DATE(expected_close_date), INTEGER(next_activity_id), DATE_TIME(add_time), DATE_TIME(update_time), STRING(visible_to), STRING(cc_email)} | OBJECT_BUILDER  |





### Search leads
Searches all leads by title, notes and/or custom fields. This endpoint is a wrapper of <a href="https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem">/v1/itemSearch</a> with a narrower OAuth scope. Found leads can be filtered by the person ID and the organization ID.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |
| Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |
| Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |
| Person Id | INTEGER | SELECT  |  Will filter leads by the provided person ID. The upper limit of found leads associated with the person is 2000.  |
| Organization Id | INTEGER | INTEGER  |  Will filter leads by the provided organization ID. The upper limit of found leads associated with the organization is 2000.  |
| Include Fields | STRING | SELECT  |  Supports including optional fields in the results which are not provided by default  |
| Start | INTEGER | INTEGER  |  Pagination start. Note that the pagination is based on main results and does not include related items when using `search_for_related_items` parameter.  |
| Limit | INTEGER | INTEGER  |  Items shown per page  |


### Output


___Sample Output:___

```{success=true, additional_data={type=object, description=The additional data of the list, properties={start={description=Pagination start, type=integer}, more_items_in_collection={description=If there are more list items in the collection than displayed or not, type=boolean}, limit={description=Items shown per page, type=integer}}}, data={items=[{result_score=0.29, item={title=John Doe lead, owner={id=1}, phones=[], visible_to=3, is_archived=false, custom_fields=[], id=39c433f0-8a4c-11ec-8728-09968f0a1ca0, value=100, person={name=John Doe, id=1}, emails=[john@doe.com], organization={name=John company, id=1}, currency=USD, notes=[], type=lead}}]}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{NUMBER(result_score), {STRING(id), STRING(type), STRING(title), {INTEGER(id)}(owner), {INTEGER(id), STRING(name)}(person), {INTEGER(id), STRING(name)}(organization), [STRING](phones), [STRING](emails), [STRING](custom_fields), [STRING](notes), INTEGER(value), STRING(currency), INTEGER(visible_to), BOOLEAN(is_archived)}(item)}](items)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| {{INTEGER(start), INTEGER(limit), BOOLEAN(more_items_in_collection), INTEGER(next_start)}(pagination)} | OBJECT_BUILDER  |





### Get all organizations
Returns all organizations.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| User Id | INTEGER | SELECT  |  If supplied, only organizations owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied.  |
| Filter Id | INTEGER | SELECT  |  The ID of the filter to use  |
| First Char | STRING | TEXT  |  If supplied, only organizations whose name starts with the specified letter will be returned (case insensitive)  |
| Start | INTEGER | INTEGER  |  Pagination start  |
| Limit | INTEGER | INTEGER  |  Items shown per page  |
| Sort | STRING | TEXT  |  The field names and sorting mode separated by a comma (`field_name_1 ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).  |


### Output


___Sample Output:___

```{related_objects={user={123={name=Jane Doe, has_pic=1, pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, id=123, active_flag=true, email=jane@pipedrive.com}}, organization={1={owner_id=123, active_flag=true, name=Org Name, people_count=1, address=Mustamäe tee 3a, 10615 Tallinn, id=1, cc_email=org@pipedrivemail.com}}, picture={1={update_time=0000-00-00 00:00:00, add_time=2020-09-08 08:17:52, id=1, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}}}, additional_data={pagination={more_items_in_collection=false, next_start=100, limit=100, start=0}}, success=true, data=[{address_route=Mustamäe tee, label=7, owner_name=John Doe, closed_deals_count=3, files_count=0, address_postal_code=10616.0, address_subpremise=, next_activity_time=11:30:00, country_code=USA, active_flag=true, update_time=2020-09-08 12:14:11, related_won_deals_count=0, open_deals_count=1, next_activity_id=128, email_messages_count=2, address_street_number=3a, people_count=1, address_formatted_address=Mustamäe tee 3a, 10616 Tallinn, Estonia, next_activity_date=2019-11-29, done_activities_count=1, activities_count=2, picture_id={update_time=0000-00-00 00:00:00, value=101, add_time=2020-09-08 08:17:52, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}, address_locality=Tallinn, won_deals_count=0, related_lost_deals_count=0, owner_id={pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, email=will.smith@pipedrive.com, name=Will Smith, has_pic=0, id=10, value=10, active_flag=true}, address_admin_area_level_2=, address_admin_area_level_1=Harju maakond, first_char=b, notes_count=0, address_country=Estonia, name=Bolt, last_activity_id=34, related_open_deals_count=2, company_id=77, cc_email=org@pipedrivemail.com, related_closed_deals_count=1, add_time=2020-02-25 10:04:08, visible_to=3.0, last_activity_date=2019-11-28, address=Mustamäe tee 3a, 10615 Tallinn, id=1, followers_count=1, undone_activities_count=0, lost_deals_count=0, address_sublocality=Kristiine}]}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER(start), INTEGER(limit), BOOLEAN(more_items_in_collection), INTEGER(next_start)}(pagination)} | OBJECT_BUILDER  |
| [{STRING(address_route), INTEGER(related_closed_deals_count), INTEGER(email_messages_count), STRING(name), INTEGER(has_pic), BOOLEAN(active_flag), INTEGER(id), INTEGER(value), STRING(email), STRING(pic_hash), STRING(cc_email), INTEGER(open_deals_count), BOOLEAN(active_flag), STRING(update_time), INTEGER(added_by_user_id), INTEGER(item_id), STRING(item_type), BOOLEAN(active_flag), INTEGER(value), STRING(add_time), {STRING(128), STRING(512)}(pictures), INTEGER(people_count), INTEGER(last_activity_id), STRING(next_activity_date), STRING(update_time), INTEGER(activities_count), INTEGER(id), STRING(address_admin_area_level_2), INTEGER(won_deals_count), STRING(address_admin_area_level_1), STRING(address_street_number), STRING(owner_name), INTEGER(files_count), STRING(address), INTEGER(company_id), STRING(address_formatted_address), STRING(address_postal_code), INTEGER(related_won_deals_count), STRING(address_country), STRING(first_char), INTEGER(undone_activities_count), INTEGER(closed_deals_count), STRING(address_subpremise), STRING(last_activity_date), INTEGER(label), INTEGER(related_open_deals_count), INTEGER(related_lost_deals_count), INTEGER(next_activity_id), STRING(country_code), STRING(visible_to), INTEGER(notes_count), INTEGER(followers_count), STRING(name), STRING(address_sublocality), STRING(address_locality), INTEGER(lost_deals_count), STRING(next_activity_time), STRING(add_time), INTEGER(done_activities_count)}] | ARRAY_BUILDER  |
| {{}(organization), {}(user), {}(picture)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |





### Add an organization
Adds a new organization. Note that you can supply additional custom fields along with the request that are not described here. These custom fields are different for each Pipedrive account and can be recognized by long hashes as keys. To determine which custom fields exists, fetch the organizationFields and look for `key` values. For more information, see the tutorial for <a href="https://pipedrive.readme.io/docs/adding-an-organization" target="_blank" rel="noopener noreferrer">adding an organization</a>.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Add Organization Request | {STRING(name), STRING(add_time), STRING(visible_to), INTEGER(owner_id)} | OBJECT_BUILDER  |  |


### Output


___Sample Output:___

```{success=true, related_objects={user={123={name=Jane Doe, has_pic=1, pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, id=123, active_flag=true, email=jane@pipedrive.com}}, organization={1={owner_id=123, active_flag=true, name=Org Name, people_count=1, address=Mustamäe tee 3a, 10615 Tallinn, id=1, cc_email=org@pipedrivemail.com}}, picture={1={update_time=0000-00-00 00:00:00, add_time=2020-09-08 08:17:52, id=1, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}}}, data={address_route=Mustamäe tee, label=7, owner_name=John Doe, closed_deals_count=3, files_count=0, address_postal_code=10616.0, address_subpremise=, next_activity_time=11:30:00, country_code=USA, active_flag=true, update_time=2020-09-08 12:14:11, related_won_deals_count=0, open_deals_count=1, next_activity_id=128, email_messages_count=2, address_street_number=3a, people_count=1, address_formatted_address=Mustamäe tee 3a, 10616 Tallinn, Estonia, next_activity_date=2019-11-29, done_activities_count=1, activities_count=2, picture_id={update_time=0000-00-00 00:00:00, value=101, add_time=2020-09-08 08:17:52, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}, address_locality=Tallinn, won_deals_count=0, related_lost_deals_count=0, owner_id={pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, email=will.smith@pipedrive.com, name=Will Smith, has_pic=0, id=10, value=10, active_flag=true}, address_admin_area_level_2=, address_admin_area_level_1=Harju maakond, first_char=b, notes_count=0, address_country=Estonia, name=Bolt, last_activity_id=34, related_open_deals_count=2, company_id=77, cc_email=org@pipedrivemail.com, related_closed_deals_count=1, add_time=2020-02-25 10:04:08, visible_to=3.0, last_activity_date=2019-11-28, address=Mustamäe tee 3a, 10615 Tallinn, id=1, followers_count=1, undone_activities_count=0, lost_deals_count=0, address_sublocality=Kristiine}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {STRING(128), STRING(512)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| {{}(organization), {}(user), {}(picture)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |





### Search organizations
Searches all organizations by name, address, notes and/or custom fields. This endpoint is a wrapper of <a href="https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem">/v1/itemSearch</a> with a narrower OAuth scope.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |
| Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |
| Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |
| Start | INTEGER | INTEGER  |  Pagination start. Note that the pagination is based on main results and does not include related items when using `search_for_related_items` parameter.  |
| Limit | INTEGER | INTEGER  |  Items shown per page  |


### Output


___Sample Output:___

```{success=true, additional_data={pagination={start=0, more_items_in_collection=false, limit=100}}, data={items=[{result_score=0.316, item={address=Mustamäe tee 3a, 10615 Tallinn, owner={id=1}, notes=[], name=Organization name, id=1, type=organization, visible_to=3, custom_fields=[]}}]}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{NUMBER(result_score), {INTEGER(id), STRING(type), STRING(name), STRING(address), INTEGER(visible_to), {INTEGER(id)}(owner), [STRING](custom_fields), [STRING](notes)}(item)}](items)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| {{INTEGER(start), INTEGER(limit), BOOLEAN(more_items_in_collection), INTEGER(next_start)}(pagination)} | OBJECT_BUILDER  |





### Delete an organization
Marks an organization as deleted. After 30 days, the organization will be permanently deleted.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Id | INTEGER | INTEGER  |  The ID of the organization  |


### Output


___Sample Output:___

```{success=true, data={id=123}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| BOOLEAN | SELECT  |
| {INTEGER(id)} | OBJECT_BUILDER  |





### Get details of an organization
Returns the details of an organization. Note that this also returns some additional fields which are not present when asking for all organizations. Also note that custom fields appear as long hashes in the resulting data. These hashes can be mapped against the `key` value of organizationFields.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Id | INTEGER | INTEGER  |  The ID of the organization  |


### Output


___Sample Output:___

```{success=true, data={allOf=[{address_route=Mustamäe tee, label=7, owner_name=John Doe, closed_deals_count=3, files_count=0, address_postal_code=10616.0, address_subpremise=, next_activity_time=11:30:00, country_code=USA, active_flag=true, update_time=2020-09-08 12:14:11, related_won_deals_count=0, open_deals_count=1, next_activity_id=128, email_messages_count=2, address_street_number=3a, people_count=1, address_formatted_address=Mustamäe tee 3a, 10616 Tallinn, Estonia, next_activity_date=2019-11-29, done_activities_count=1, activities_count=2, picture_id={update_time=0000-00-00 00:00:00, value=101, add_time=2020-09-08 08:17:52, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}, address_locality=Tallinn, won_deals_count=0, related_lost_deals_count=0, owner_id={pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, email=will.smith@pipedrive.com, name=Will Smith, has_pic=0, id=10, value=10, active_flag=true}, address_admin_area_level_2=, address_admin_area_level_1=Harju maakond, first_char=b, notes_count=0, address_country=Estonia, name=Bolt, last_activity_id=34, related_open_deals_count=2, company_id=77, cc_email=org@pipedrivemail.com, related_closed_deals_count=1, add_time=2020-02-25 10:04:08, visible_to=3.0, last_activity_date=2019-11-28, address=Mustamäe tee 3a, 10615 Tallinn, id=1, followers_count=1, undone_activities_count=0, lost_deals_count=0, address_sublocality=Kristiine}, {properties={related_objects={user={123={name=Jane Doe, has_pic=1, pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, id=123, active_flag=true, email=jane@pipedrive.com}}, organization={1={owner_id=123, active_flag=true, name=Org Name, people_count=1, address=Mustamäe tee 3a, 10615 Tallinn, id=1, cc_email=org@pipedrivemail.com}}, picture={1={update_time=0000-00-00 00:00:00, add_time=2020-09-08 08:17:52, id=1, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}}}, last_activity={marked_as_done_time=2020-08-08 08:08:38, lead_id=46c3b0e1-db35-59ca-1828-4817378dff71, location=Mustamäe tee 3, Tallinn, Estonia, calendar_sync_include_context=, location_sublocality=Kristiine, assigned_to_user_id=1235, org_name=Organization, active_flag=true, update_time=2020-08-08 12:37:56, busy_flag=true, conference_meeting_client=871b8bc88d3a1202, last_notification_time=2020-08-08 12:37:56, location_formatted_address=Mustamäe tee 3, 10616 Tallinn, Estonia, note=A note for the activity, done=false, gcal_event_id=, file={id=376892,, url=https://pipedrive-files.s3-eu-west-1.amazonaws.com/Audio-recording.m4a, clean_name=Audio 10:55:07.m4a}, rec_master_activity_id=1, location_subpremise=, id=8, location_locality=Tallinn, update_user_id=5596, due_time=10:00, reference_id=7, org_id=5, person_id=1101, deal_dropbox_bcc=company+deal300@pipedrivemail.com, location_street_number=3.0, attendees=[{name=Attendee, user_id=, is_organizer=0, status=noreply, person_id=25312, email_address=attendee@pipedrivemail.com}], series=[], conference_meeting_url=https://pipedrive.zoom.us/link, public_description=This is a description, notification_language_id=1, type=deadline, last_notification_user_id=7655, company_id=22122, duration=01:00, conference_meeting_id=1.758746701E9, person_dropbox_bcc=company@pipedrivemail.com, owner_name=Creator, location_postal_code=10616.0, created_by_user_id=1234, person_name=Person, add_time=2020-06-08 12:37:56, deal_id=300, reference_type=scheduler-service, source_timezone=, rec_rule=RRULE:FREQ=WEEKLY;BYDAY=WE, google_calendar_id=, deal_title=Deal, location_country=Estonia, due_date=2020-06-09, subject=Deadline, participants=[{primary_flag=false, person_id=17985}, {primary_flag=true, person_id=1101}], location_admin_area_level_2=, google_calendar_etag=, user_id=1234, location_admin_area_level_1=Harju maakond, rec_rule_extension=, location_route=Mustamäe tee}, next_activity=, additional_data={pagination={more_items_in_collection=false, next_start=100, limit=100, start=0}}}, type=object}]}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{STRING(name), INTEGER(id), INTEGER(user_id), STRING(email), STRING(pic_hash)}(followers), STRING(dropbox_email)} | OBJECT_BUILDER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {STRING(128), STRING(512)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {} | OBJECT_BUILDER  |
| {} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| {{}(organization), {}(user), {}(picture)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |





### Get all persons
Returns all persons.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| User Id | INTEGER | SELECT  |  If supplied, only persons owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied.  |
| Filter Id | INTEGER | SELECT  |  The ID of the filter to use  |
| First Char | STRING | TEXT  |  If supplied, only persons whose name starts with the specified letter will be returned (case insensitive)  |
| Start | INTEGER | INTEGER  |  Pagination start  |
| Limit | INTEGER | INTEGER  |  Items shown per page  |
| Sort | STRING | TEXT  |  The field names and sorting mode separated by a comma (`field_name_1 ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).  |


### Output


___Sample Output:___

```{related_objects={user={123={name=Jane Doe, has_pic=1, pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, id=123, active_flag=true, email=jane@pipedrive.com}}, organization={1={owner_id=123, active_flag=true, name=Org Name, people_count=1, address=Mustamäe tee 3a, 10615 Tallinn, id=1, cc_email=org@pipedrivemail.com}}, picture={1={update_time=0000-00-00 00:00:00, add_time=2020-09-08 08:17:52, id=1, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}}}, additional_data={pagination={more_items_in_collection=false, next_start=100, limit=100, start=0}}, success=true, data=[{primary_email=12345@email.com, owner_id={pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, email=jane@pipedrive.com, name=Jane Doe, has_pic=1, id=123, value=123, active_flag=true}, related_won_deals_count=3, id=1, owner_name=Jane Doe, next_activity_time=11:30:00, next_activity_id=128, files_count=2, last_outgoing_mail_time=2019-05-30 03:45:35, add_time=2017-10-18 13:23:07, first_name=Will, notes_count=2, participant_closed_deals_count=1, email=[{value=12345@email.com, primary=true, label=work}], lost_deals_count=1, won_deals_count=3, related_open_deals_count=2, next_activity_date=2019-11-29, org_name=Organization name, visible_to=3.0, active_flag=true, activities_count=1, phone=[{value=12345.0, primary=true, label=work}], done_activities_count=1, last_incoming_mail_time=2019-05-29 18:21:42, related_closed_deals_count=3, undone_activities_count=2, first_char=w, related_lost_deals_count=1, followers_count=3, label=1, cc_email=org@pipedrivemail.com, last_activity_date=2019-11-28, last_activity_id=34, update_time=2020-05-08 05:30:20, name=Will Smith, closed_deals_count=3, company_id=12, picture_id={update_time=0000-00-00 00:00:00, value=4, add_time=2020-09-08 08:17:52, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}, email_messages_count=1, open_deals_count=2, marketing_status=no_consent, org_id={owner_id=123, active_flag=true, name=Org Name, people_count=1, address=Mustamäe tee 3a, 10615 Tallinn, value=1234, cc_email=org@pipedrivemail.com}, last_name=Smith, participant_open_deals_count=1}]}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {{INTEGER(start), INTEGER(limit), BOOLEAN(more_items_in_collection), INTEGER(next_start)}(pagination)} | OBJECT_BUILDER  |
| [{INTEGER(related_closed_deals_count), INTEGER(email_messages_count), STRING(cc_email), STRING(name), INTEGER(has_pic), BOOLEAN(active_flag), INTEGER(id), INTEGER(value), STRING(email), STRING(pic_hash), INTEGER(open_deals_count), STRING(last_outgoing_mail_time), BOOLEAN(active_flag), STRING(update_time), INTEGER(added_by_user_id), INTEGER(item_id), STRING(item_type), BOOLEAN(active_flag), INTEGER(id), STRING(add_time), {STRING(128), STRING(512)}(pictures), INTEGER(last_activity_id), STRING(next_activity_date), STRING(update_time), INTEGER(activities_count), INTEGER(id), STRING(org_name), STRING(first_name), [{STRING(value), BOOLEAN(primary), STRING(label)}](email), INTEGER(won_deals_count), STRING(owner_name), INTEGER(files_count), INTEGER(company_id), INTEGER(related_won_deals_count), STRING(last_incoming_mail_time), STRING(first_char), INTEGER(undone_activities_count), INTEGER(closed_deals_count), STRING(last_name), STRING(last_activity_date), INTEGER(label), INTEGER(related_open_deals_count), INTEGER(related_lost_deals_count), INTEGER(next_activity_id), [{STRING(value), BOOLEAN(primary), STRING(label)}](phone), STRING(visible_to), STRING(address), INTEGER(owner_id), STRING(cc_email), STRING(name), BOOLEAN(active_flag), INTEGER(people_count), INTEGER(value), INTEGER(notes_count), INTEGER(followers_count), STRING(name), INTEGER(lost_deals_count), STRING(next_activity_time), STRING(add_time), INTEGER(done_activities_count)}] | ARRAY_BUILDER  |
| {{}(organization), {}(user), {}(picture)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |





### Add a person
Adds a new person. Note that you can supply additional custom fields along with the request that are not described here. These custom fields are different for each Pipedrive account and can be recognized by long hashes as keys. To determine which custom fields exists, fetch the personFields and look for `key` values.<br>If a company uses the [Campaigns product](https://pipedrive.readme.io/docs/campaigns-in-pipedrive-api), then this endpoint will also accept and return the `data.marketing_status` field.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Add Person Request | {STRING(marketing_status), [{STRING(value), BOOLEAN(primary), STRING(label)}](phone), STRING(visible_to), INTEGER(owner_id), INTEGER(org_id), STRING(name), STRING(add_time), [{STRING(value), BOOLEAN(primary), STRING(label)}](email)} | OBJECT_BUILDER  |  |


### Output


___Sample Output:___

```{success=true, related_objects={user={123={name=Jane Doe, has_pic=1, pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, id=123, active_flag=true, email=jane@pipedrive.com}}}, data={primary_email=12345@email.com, owner_id={pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, email=jane@pipedrive.com, name=Jane Doe, has_pic=1, id=123, value=123, active_flag=true}, related_won_deals_count=3, id=1, owner_name=Jane Doe, next_activity_time=11:30:00, next_activity_id=128, files_count=2, last_outgoing_mail_time=2019-05-30 03:45:35, add_time=2017-10-18 13:23:07, first_name=Will, notes_count=2, participant_closed_deals_count=1, email=[{value=12345@email.com, primary=true, label=work}], lost_deals_count=1, won_deals_count=3, related_open_deals_count=2, next_activity_date=2019-11-29, org_name=Organization name, visible_to=3.0, active_flag=true, activities_count=1, phone=[{value=12345.0, primary=true, label=work}], done_activities_count=1, last_incoming_mail_time=2019-05-29 18:21:42, related_closed_deals_count=3, undone_activities_count=2, first_char=w, related_lost_deals_count=1, followers_count=3, label=1, cc_email=org@pipedrivemail.com, last_activity_date=2019-11-28, last_activity_id=34, update_time=2020-05-08 05:30:20, name=Will Smith, closed_deals_count=3, company_id=12, picture_id={update_time=0000-00-00 00:00:00, value=4, add_time=2020-09-08 08:17:52, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}, email_messages_count=1, open_deals_count=2, marketing_status=no_consent, org_id={owner_id=123, active_flag=true, name=Org Name, people_count=1, address=Mustamäe tee 3a, 10615 Tallinn, value=1234, cc_email=org@pipedrivemail.com}, last_name=Smith, participant_open_deals_count=1}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {STRING(128), STRING(512)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(value), BOOLEAN(primary), STRING(label)}] | ARRAY_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| [{STRING(value), BOOLEAN(primary), STRING(label)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| {{}(user)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |





### Search persons
Searches all persons by name, email, phone, notes and/or custom fields. This endpoint is a wrapper of <a href="https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem">/v1/itemSearch</a> with a narrower OAuth scope. Found persons can be filtered by organization ID.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Term | STRING | TEXT  |  The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.  |
| Fields | STRING | SELECT  |  A comma-separated string array. The fields to perform the search from. Defaults to all of them.  |
| Exact Match | BOOLEAN | SELECT  |  When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.  |
| Organization Id | INTEGER | INTEGER  |  Will filter persons by the provided organization ID. The upper limit of found persons associated with the organization is 2000.  |
| Include Fields | STRING | SELECT  |  Supports including optional fields in the results which are not provided by default  |
| Start | INTEGER | INTEGER  |  Pagination start. Note that the pagination is based on main results and does not include related items when using `search_for_related_items` parameter.  |
| Limit | INTEGER | INTEGER  |  Items shown per page  |


### Output


___Sample Output:___

```{success=true, additional_data={pagination={start=0, more_items_in_collection=false, limit=100}}, data={items=[{result_score=0.5092, item={visible_to=3, emails=[jane@pipedrive.com], phones=[+372 555555555], owner={id=1}, id=1, notes=[], organization={id=1, address=, name=Organization name}, type=person, custom_fields=[], name=Jane Doe}}]}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {[{NUMBER(result_score), {INTEGER(id), STRING(type), STRING(name), [STRING](phones), [STRING](emails), INTEGER(visible_to), {INTEGER(id)}(owner), {INTEGER(id), STRING(name)}(organization), [STRING](custom_fields), [STRING](notes)}(item)}](items)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |
| {{INTEGER(start), INTEGER(limit), BOOLEAN(more_items_in_collection), INTEGER(next_start)}(pagination)} | OBJECT_BUILDER  |





### Delete a person
Marks a person as deleted. After 30 days, the person will be permanently deleted.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Id | INTEGER | INTEGER  |  The ID of the person  |


### Output


___Sample Output:___

```{success=true, data={id=12}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {INTEGER(id)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |





### Get details of a person
Returns the details of a person. Note that this also returns some additional fields which are not present when asking for all persons. Also note that custom fields appear as long hashes in the resulting data. These hashes can be mapped against the `key` value of personFields.<br>If a company uses the [Campaigns product](https://pipedrive.readme.io/docs/campaigns-in-pipedrive-api), then this endpoint will also return the `data.marketing_status` field.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Id | INTEGER | INTEGER  |  The ID of the person  |


### Output


___Sample Output:___

```{related_objects={user={123={name=Jane Doe, has_pic=1, pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, id=123, active_flag=true, email=jane@pipedrive.com}}, organization={1={owner_id=123, active_flag=true, name=Org Name, people_count=1, address=Mustamäe tee 3a, 10615 Tallinn, id=1, cc_email=org@pipedrivemail.com}}, picture={1={update_time=0000-00-00 00:00:00, add_time=2020-09-08 08:17:52, id=1, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}}}, additional_data={dropbox_email=test@email.com}, success=true, data={primary_email=12345@email.com, owner_id={pic_hash=2611ace8ac6a3afe2f69ed56f9e08c6b, email=jane@pipedrive.com, name=Jane Doe, has_pic=1, id=123, value=123, active_flag=true}, related_won_deals_count=3, id=1, owner_name=Jane Doe, next_activity_time=11:30:00, next_activity_id=128, files_count=2, last_outgoing_mail_time=2019-05-30 03:45:35, add_time=2017-10-18 13:23:07, first_name=Will, notes_count=2, participant_closed_deals_count=1, email=[{value=12345@email.com, primary=true, label=work}], lost_deals_count=1, won_deals_count=3, related_open_deals_count=2, next_activity_date=2019-11-29, org_name=Organization name, visible_to=3.0, active_flag=true, activities_count=1, phone=[{value=12345.0, primary=true, label=work}], done_activities_count=1, last_incoming_mail_time=2019-05-29 18:21:42, related_closed_deals_count=3, undone_activities_count=2, first_char=w, related_lost_deals_count=1, followers_count=3, label=1, cc_email=org@pipedrivemail.com, last_activity_date=2019-11-28, last_activity_id=34, update_time=2020-05-08 05:30:20, name=Will Smith, closed_deals_count=3, company_id=12, picture_id={update_time=0000-00-00 00:00:00, value=4, add_time=2020-09-08 08:17:52, pictures={128=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg, 512=https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg}, added_by_user_id=967055, item_id=25, item_type=person, active_flag=true}, email_messages_count=1, open_deals_count=2, marketing_status=no_consent, org_id={owner_id=123, active_flag=true, name=Org Name, people_count=1, address=Mustamäe tee 3a, 10615 Tallinn, value=1234, cc_email=org@pipedrivemail.com}, last_name=Smith, participant_open_deals_count=1}}```



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING(dropbox_email)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| {STRING(128), STRING(512)} | OBJECT_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| [{STRING(value), BOOLEAN(primary), STRING(label)}] | ARRAY_BUILDER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| [{STRING(value), BOOLEAN(primary), STRING(label)}] | ARRAY_BUILDER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| BOOLEAN | SELECT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| {{}(organization), {}(user), {}(picture)} | OBJECT_BUILDER  |
| BOOLEAN | SELECT  |





