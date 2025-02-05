---
title: "Image Helper"
description: "Helper component which contains various actions for image manipulation."
---

Helper component which contains various actions for image manipulation.


Categories: helpers


Type: imageHelper/v1

<hr />




## Actions


### Compress Image
Compress image with specified quality.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| image | Image | FILE_ENTRY | FILE_ENTRY  |  | true  |
| quality | Quality | NUMBER | NUMBER  |  Compression quality of the image.  |  true  |
| resultFileName | Result File Name | STRING | TEXT  |  Specifies the output file name for the result image.  |  true  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |






### Crop Image
Crops an image to the specified dimensions.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| image | Image | FILE_ENTRY | FILE_ENTRY  |  | true  |
| x | X Coordinate | INTEGER | INTEGER  |  The horizontal starting point of the crop area  |  true  |
| y | Y Coordinate | INTEGER | INTEGER  |  The vertical starting point of the crop area  |  true  |
| width | Width | INTEGER | INTEGER  |  Width of the crop area  |  true  |
| height | Height | INTEGER | INTEGER  |  Height of the crop area  |  true  |
| resultFileName | Result File Name | STRING | TEXT  |  Specifies the output file name for the result image.  |  true  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |






### Get Image Metadata
Get metadata of the image.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| image | Image | FILE_ENTRY | FILE_ENTRY  |  | true  |
| resultFileName | Result File Name | STRING | TEXT  |  Specifies the output file name for the result image.  |  true  |




### Image to Base64
Converts image to Base64 string.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| image | Image | FILE_ENTRY | FILE_ENTRY  |  | true  |


#### Output



Type: STRING







### Resize Image
Resizes an image to the specified width and height.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| image | Image | FILE_ENTRY | FILE_ENTRY  |  | true  |
| width | Width | INTEGER | INTEGER  |  Width in pixels  |  true  |
| height | Height | INTEGER | INTEGER  |  Height in pixels  |  true  |
| resultFileName | Result File Name | STRING | TEXT  |  Specifies the output file name for the result image.  |  true  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |






### Rotate Image
Rotates an image by a specified degree.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| image | Image | FILE_ENTRY | FILE_ENTRY  |  | true  |
| degree | Degree | INTEGER | SELECT  |  Specifies the degree of clockwise rotation applied to the image.  |  true  |
| resultFileName | Result File Name | STRING | TEXT  |  Specifies the output file name for the result image.  |  true  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |








