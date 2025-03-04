---
title: "Beamer"
description: "Beamer is a customer engagement platform that helps businesses communicate updates, collect feedback, and boost user engagement through in-app notifications, changelogs, and announcements."
---

Beamer is a customer engagement platform that helps businesses communicate updates, collect feedback, and boost user engagement through in-app notifications, changelogs, and announcements.


Categories: productivity-and-collaboration


Type: beamer/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | API key | STRING |  | true |





<hr />



## Actions


### Create Feature Request
Name: createFeatureRequest

Creates a new feature request.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| title | Feature Request Title | STRING | The name of the new feature request. | true |
| content | Feature Request Content | STRING | The content of the new feature request. | false |
| userEmail | User Email | STRING | The email of the user that is creating the new feature request. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the new feature request. |
| date | STRING | Publish date of the new feature request. |
| visible | STRING | Whether this feature required is visible or not. |
| category | STRING | The category of the new feature request. |
| status | STRING | The status of the new feature request. |
| translations | ARRAY <details> <summary> Items </summary> [{STRING\(title), STRING\(content), STRING\(contentHtml), STRING\(language), STRING\(permalink), [STRING]\(images)}] </details> |  |
| votesCount | INTEGER | The number of votes for the new feature request. |
| commentsCount | STRING | The number of comments for the new feature request. |
| notes | STRING | The notes for the new feature request. |
| filters | STRING | Segment filters for the new feature request. |
| internalUserEmail | STRING | Email of the user in your account who created this feature request (if created by a team member). |
| internalUserFirstname | STRING | First name of the user in your account who created this feature request (if created by a team member). |
| internalUserLastname | STRING | Last name of the user in your account who created this feature request (if created by a team member). |
| userId | STRING | ID of the end user who created this feature request (if created by an end user). |
| userEmail | STRING | Email of the end user who created this feature request (if created by an end user). |
| userFirstname | STRING | First name of the end user who created this feature request (if created by an end user). |
| userLastname | STRING | Last name of the end user who created this feature request (if created by an end user). |




#### JSON Example
```json
{
  "label" : "Create Feature Request",
  "name" : "createFeatureRequest",
  "parameters" : {
    "title" : "",
    "content" : "",
    "userEmail" : ""
  },
  "type" : "beamer/v1/createFeatureRequest"
}
```


### Create Post
Name: createPost

Creates a new post.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| title | Title | STRING | Title of the new post. | true |
| content | Content | STRING | Content of the new post. | true |
| category | Category | STRING <details> <summary> Options </summary> new, improvement, fix, comingsoon, announcement, other </details> | Category of the new post. | true |
| userEmail | User Email | STRING | Email of the user that is creating the new post. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the new post. |
| date | STRING | Publication date of the new post. |
| dueDate | STRING | Expiration date of the new post. |
| published | STRING | Whether the new post is published or a draft. |
| category | STRING | Category of the new post. |
| feedbackEnabled | STRING | Whether this user feedback is enabled for this post. |
| reactionsEnabled | STRING | Whether reactions are enabled for this post. |
| translations | ARRAY <details> <summary> Items </summary> [{STRING\(title), STRING\(content), STRING\(contentHtml), STRING\(language), STRING\(category), STRING\(linkUrl), STRING\(linkText), [STRING]\(images)}] </details> |  |




#### JSON Example
```json
{
  "label" : "Create Post",
  "name" : "createPost",
  "parameters" : {
    "title" : "",
    "content" : "",
    "category" : "",
    "userEmail" : ""
  },
  "type" : "beamer/v1/createPost"
}
```


### Get Feed
Name: getFeed

Get the URL for your feed.


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| url | STRING | URL for your standalone feed. |




#### JSON Example
```json
{
  "label" : "Get Feed",
  "name" : "getFeed",
  "type" : "beamer/v1/getFeed"
}
```


### New Comment
Name: newComment

Creates a new comment on selected post.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| postId | Post | STRING | ID of the post that will have the new comment. | true |
| text | Text | STRING | Text of the comment. | false |
| userId | User ID | STRING | ID of the user that is creating the new comment. | false |
| userEmail | User Email | STRING | Email of the user that is creating the new comment. | false |
| userFirstname | User First Name | STRING | First name of the user that is creating the new comment. | false |
| userLastname | User Last Name | STRING | Last name of the user that is creating the new comment. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | ID of the new comment. |
| date | STRING | Publication date of the new comment. |
| text | STRING | Content of the new comment. |
| postTitle | STRING | Title of the post this comment was created on. |
| userId | STRING | ID of the user that created the new comment. |
| userEmail | STRING | Email of the user that created the new comment. |
| userFirstname | STRING | First name of the user that created the new comment. |
| userLastname | STRING | Last name of the user that created the new comment. |
| url | STRING | URL of the new comment in your dashboard. |




#### JSON Example
```json
{
  "label" : "New Comment",
  "name" : "newComment",
  "parameters" : {
    "postId" : "",
    "text" : "",
    "userId" : "",
    "userEmail" : "",
    "userFirstname" : "",
    "userLastname" : ""
  },
  "type" : "beamer/v1/newComment"
}
```


### New Vote
Name: newVote

Creates a new vote on selected feature request.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| requestId | Feature Request ID | STRING | ID of the feature request that will have the new vote. | true |
| userId | User ID | STRING | ID of the user that is creating the new vote. | false |
| userEmail | User Email | STRING | Email of the user that is creating the new vote. | false |
| userFirstname | User First Name | STRING | First name of the user that is creating the new vote. | false |
| userLastname | User Last Name | STRING | Last name of the user that is creating the new vote. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| id | STRING | The ID of the new vote. |
| date | STRING | Creation date of the new vote. |
| featureRequestTitle | STRING | Title of the feature request this vote is created on. |
| userId | STRING | ID of the user that created the new vote. |
| userEmail | STRING | Email of the user that created the new vote. |
| userFirstname | STRING | First name of the user that created the new vote. |
| userLastname | STRING | Last name of the user that created the new vote. |
| url | STRING | URL of the new vote in your dashboard. |




#### JSON Example
```json
{
  "label" : "New Vote",
  "name" : "newVote",
  "parameters" : {
    "requestId" : "",
    "userId" : "",
    "userEmail" : "",
    "userFirstname" : "",
    "userLastname" : ""
  },
  "type" : "beamer/v1/newVote"
}
```




