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
Creates a new presentation based on an existing one and can replace any placeholder variables found in your template presentation, like [[name]], [[email]], etc.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Template Presentation ID | STRING | SELECT  |  The ID of the template presentation from which the new presentation will be created.  |
| New Presentation Name | STRING | TEXT  |  Name of the new presentation.  |
| Folder for New Presentation | STRING | SELECT  |  Folder ID where the new presentation will be saved. If not provided, the new presentation will be saved in the same folder as the template presentation.  |
| Values | {} | OBJECT_BUILDER  |  Don't include the "[[]]", only the key name and its value.  |




