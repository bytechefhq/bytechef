---
title: "Image Helper"
description: "Helper component which contains various actions for image manipulation."
---
## Reference
<hr />

Helper component which contains various actions for image manipulation.


Categories: [helpers]


Version: 1

<hr />






## Actions


### Image to Base64
Converts image to Base64 string.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Image | FILE_ENTRY | FILE_ENTRY  |  |


### Output



Type: STRING







### Resize Image
Resizes an image to the specified width and height.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Image | FILE_ENTRY | FILE_ENTRY  |  |
| Width | INTEGER | INTEGER  |  Width in pixels  |
| Height | INTEGER | INTEGER  |  Height in pixels  |
| Result File Name | STRING | TEXT  |  Specifies the output file name for the result image.  |


### Output



Type: FILE_ENTRY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Rotate Image
Rotates an image by a specified degree.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Image | FILE_ENTRY | FILE_ENTRY  |  |
| Degree | INTEGER | SELECT  |  Specifies the degree of clockwise rotation applied to the image.  |
| Result File Name | STRING | TEXT  |  Specifies the output file name for the result image.  |


### Output



Type: FILE_ENTRY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






### Crop Image
Crop an image to the specified input parameters.

#### Properties

|       Name       |    Type    |     Control Type     |                       Description                        |
|:----------------:|:----------:|:--------------------:|:--------------------------------------------------------:|
|      Image       | FILE_ENTRY | FILE_ENTRY  |                                                          |
|   X coordinate   |  INTEGER   | INTEGER  | Specifies the horizontal starting point of the crop area |
|   Y coordinate   |  INTEGER   | INTEGER  |  Specifies the vertical starting point of the crop area  |
|      Width       |  INTEGER   | INTEGER  |           Specifies the width of the crop area           |
|      Height      |  INTEGER   | INTEGER  |          Specifies the height of the crop area           |
| Result File Name | STRING | TEXT  |  Specifies the output file name for the result image.  |


### Output



Type: FILE_ENTRY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |



