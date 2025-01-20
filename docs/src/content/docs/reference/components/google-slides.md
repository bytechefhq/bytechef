---
title: "Google Slides"
description: "Google Slides is a cloud-based presentation software that allows users to create, edit, and collaborate on presentations online in real-time."
---
## Reference
<hr />

Google Slides is a cloud-based presentation software that allows users to create, edit, and collaborate on presentations online in real-time.


Categories: [file-storage]


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





## Actions


### Create Presentation Based on Template
Create a presentation based on a template and replace the values with the ones provided.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Presentation ID | STRING | SELECT  |  The ID of the presentation to replace the values.  |
| New Presentation Name | STRING | TEXT  |  Name of the new presentation.  |
| Values | {} | OBJECT_BUILDER  |  Don't include the "[[]]", only the key name and its value.  |




