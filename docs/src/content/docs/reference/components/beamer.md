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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | API key | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Feature Request
Name: createFeatureRequest

Creates a new feature request.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| title | Feature Request Title | STRING | TEXT  |  The name of the new feature request.  |  true  |
| content | Feature Request Content | STRING | TEXT  |  The content of the new feature request.  |  false  |
| userEmail | User Email | STRING | TEXT  |  The email of the user that is creating the new feature request.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| date | STRING | TEXT  |
| visible | STRING | TEXT  |
| category | STRING | TEXT  |
| status | STRING | TEXT  |
| translations | [{STRING\(title), STRING\(content), STRING\(contentHtml), STRING\(language), STRING\(permalink), [STRING]\(images)}] | ARRAY_BUILDER  |
| votesCount | INTEGER | INTEGER  |
| commentsCount | STRING | TEXT  |
| notes | STRING | TEXT  |
| filters | STRING | TEXT  |
| internalUserEmail | STRING | TEXT  |
| internalUserFirstname | STRING | TEXT  |
| internalUserLastname | STRING | TEXT  |
| userId | STRING | TEXT  |
| userEmail | STRING | TEXT  |
| userFirstname | STRING | TEXT  |
| userLastname | STRING | TEXT  |






### Create Post
Name: createPost

Creates a new post.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| title | Title | STRING | TEXT  |  Title of the new post.  |  true  |
| content | Content | STRING | TEXT  |  Content of the new post.  |  true  |
| category | Category | STRING | SELECT  |  Category of the new post.  |  true  |
| userEmail | User Email | STRING | TEXT  |  Email of the user that is creating the new post.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| date | STRING | TEXT  |
| dueDate | STRING | TEXT  |
| published | STRING | TEXT  |
| category | STRING | TEXT  |
| feedbackEnabled | STRING | TEXT  |
| reactionsEnabled | STRING | TEXT  |
| translations | [{STRING\(title), STRING\(content), STRING\(category), STRING\(contentHtml), STRING\(language), STRING\(postUrl)}] | ARRAY_BUILDER  |






### Get Feed
Name: getFeed

Get the URL for your feed.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
null


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| url | STRING | TEXT  |






### New Comment
Name: newComment

Creates a new comment on selected post.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| postId | Post | STRING | SELECT  |  ID of the post that will have the new comment.  |  true  |
| text | Text | STRING | TEXT  |  Text of the comment.  |  false  |
| userId | User ID | STRING | TEXT  |  ID of the user that is creating the new comment.  |  false  |
| userEmail | User Email | STRING | TEXT  |  Email of the user that is creating the new comment.  |  false  |
| userFirstname | User First Name | STRING | TEXT  |  First name of the user that is creating the new comment.  |  false  |
| userLastname | User Last Name | STRING | TEXT  |  Last name of the user that is creating the new comment.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| date | STRING | TEXT  |
| text | STRING | TEXT  |
| postTitle | STRING | TEXT  |
| userId | STRING | TEXT  |
| userEmail | STRING | TEXT  |
| userFirstname | STRING | TEXT  |
| userLastname | STRING | TEXT  |
| url | STRING | TEXT  |






### New Vote
Name: newVote

Creates a new vote on selected feature request.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| requestId | Feature Request ID | STRING | SELECT  |  ID of the feature request that will have the new vote.  |  true  |
| userId | User ID | STRING | TEXT  |  ID of the user that is creating the new vote.  |  false  |
| userEmail | User Email | STRING | TEXT  |  Email of the user that is creating the new vote.  |  false  |
| userFirstname | User First Name | STRING | TEXT  |  First name of the user that is creating the new vote.  |  false  |
| userLastname | User Last Name | STRING | TEXT  |  Last name of the user that is creating the new vote.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| date | STRING | TEXT  |
| featureRequestTitle | STRING | TEXT  |
| userId | STRING | TEXT  |
| userEmail | STRING | TEXT  |
| userFirstname | STRING | TEXT  |
| userLastname | STRING | TEXT  |
| url | STRING | TEXT  |








