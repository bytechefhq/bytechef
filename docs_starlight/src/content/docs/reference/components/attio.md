---
title: "Attio"
description: "Attio is the AI-native CRM that builds, scales and grows your company to the next level."
---

Attio is the AI-native CRM that builds, scales and grows your company to the next level.


Categories: CRM


Type: attio/v1

<hr />



## Connections

Version: 1


### Access Token

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| token | Access Token | STRING | Can be found in Workspace Settings -> Developers -> Access tokens. | true |





<hr />



## Actions


### Create Record
Name: createRecord

Creates a new record.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| record_type | Record Type | STRING | Type of record that will be created. | true |
| value | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> record_type </details> |  | true |

#### Example JSON Structure
```json
{
  "label" : "Create Record",
  "name" : "createRecord",
  "parameters" : {
    "record_type" : "",
    "value" : { }
  },
  "type" : "attio/v1/createRecord"
}
```

#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.




### Create Task
Name: createTask

Creates a new task.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| content | Content | STRING |  | true |
| format | Format | STRING <details> <summary> Options </summary> plaintext </details> |  | true |
| deadline_at | Deadline | DATE_TIME |  | true |
| is_completed | Is Completed | BOOLEAN <details> <summary> Options </summary> true, false </details> |  | true |
| linked_records | Linked Records | ARRAY <details> <summary> Items </summary> [{STRING\(target_object), STRING\(target_record_id)}] </details> |  | true |
| assignees | Assignees | ARRAY <details> <summary> Items </summary> [{STRING\(referenced_actor_type), STRING\(referenced_actor_id)}] </details> |  | true |

#### Example JSON Structure
```json
{
  "label" : "Create Task",
  "name" : "createTask",
  "parameters" : {
    "content" : "",
    "format" : "",
    "deadline_at" : "2021-01-01T00:00:00",
    "is_completed" : false,
    "linked_records" : [ {
      "target_object" : "",
      "target_record_id" : ""
    } ],
    "assignees" : [ {
      "referenced_actor_type" : "",
      "referenced_actor_id" : ""
    } ]
  },
  "type" : "attio/v1/createTask"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| data | OBJECT <details> <summary> Properties </summary> {{STRING\(workspace_id), STRING\(task_id)}\(id), STRING\(content_plaintext), BOOLEAN\(is_completed), STRING\(deadline_at), [STRING\($target_object_id), STRING\($target_record_id)]\(linked_records), [STRING\($referenced_actor_type), STRING\($referenced_actor_id)]\(assignees), {STRING\(type), STRING\(id)}\(created_by_actor), STRING\(created_at)} </details> |  |




#### Output Example
```json
{
  "data" : {
    "id" : {
      "workspace_id" : "",
      "task_id" : ""
    },
    "content_plaintext" : "",
    "is_completed" : false,
    "deadline_at" : "",
    "linked_records" : [ "", "" ],
    "assignees" : [ "", "" ],
    "created_by_actor" : {
      "type" : "",
      "id" : ""
    },
    "created_at" : ""
  }
}
```


### Update Record
Name: updateRecord

Updates a record.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| record_type | Record Type | STRING | Type of record that will be created. | true |
| record_id | Record ID | STRING <details> <summary> Depends On </summary> record_type </details> | ID of the record that will be updated. | true |
| value | | DYNAMIC_PROPERTIES <details> <summary> Depends On </summary> record_type </details> |  | true |

#### Example JSON Structure
```json
{
  "label" : "Update Record",
  "name" : "updateRecord",
  "parameters" : {
    "record_type" : "",
    "record_id" : "",
    "value" : { }
  },
  "type" : "attio/v1/updateRecord"
}
```

#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.






## Triggers


### Record Created
Name: recordCreated

Triggers when new record is created.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| event_type | STRING | Type of an event that triggers the trigger. |
| id | OBJECT <details> <summary> Properties </summary> {STRING\(workspace_id), STRING\(task_id)} </details> |  |
| actor | OBJECT <details> <summary> Properties </summary> {STRING\(type), STRING\(id)} </details> |  |




#### JSON Example
```json
{
  "label" : "Record Created",
  "name" : "recordCreated",
  "type" : "attio/v1/recordCreated"
}
```


### Task Created
Name: taskCreated

Triggers when new task is created.

Type: DYNAMIC_WEBHOOK


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| event_type | STRING | Type of an event that triggers the trigger. |
| id | OBJECT <details> <summary> Properties </summary> {STRING\(workspace_id), STRING\(task_id)} </details> |  |
| actor | OBJECT <details> <summary> Properties </summary> {STRING\(type), STRING\(id)} </details> |  |




#### JSON Example
```json
{
  "label" : "Task Created",
  "name" : "taskCreated",
  "type" : "attio/v1/taskCreated"
}
```


<hr />

<hr />

# Additional instructions
<hr />

