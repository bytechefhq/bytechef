---
title: "Stability AI"
description: "Activating humanity's potential through generative AI. Open models in every modality, for everyone, everywhere."
---

Activating humanity's potential through generative AI. Open models in every modality, for everyone, everywhere.


Categories: artificial-intelligence


Type: stability/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | API Key | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Image
Create an image using text-to-image models

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | TEXT  |  The model to use for image generation.  |  false  |
| imageMessages | Messages | [{STRING\(content), NUMBER\(weight)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| height | Height | INTEGER | INTEGER  |  Height of the image to generate, in pixels, in an increment divisible by 64. Engine-specific dimension validation applies.  |  true  |
| width | Width | INTEGER | INTEGER  |  Width of the image to generate, in pixels, in an increment divisible by 64. Engine-specific dimension validation applies.  |  true  |
| n | Number of Responses | INTEGER | INTEGER  |  The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported..  |  null  |
| responseFormat | Response format | STRING | SELECT  |  The format in which the generated images are returned.  |  null  |
| style | Style | STRING | SELECT  |  Pass in a style preset to guide the image model towards a particular style. This list of style presets is subject to change.  |  null  |
| steps | Steps | INTEGER | INTEGER  |  Number of diffusion steps to run. Valid range: 10 to 50.  |  null  |
| cfgScale | CFG scale | NUMBER | NUMBER  |  The strictness level of the diffusion process adherence to the prompt text. Range: 0 to 35.  |  null  |
| clipGuidancePreset | Clip guidance preset | STRING | TEXT  |  Pass in a style preset to guide the image model towards a particular style. This list of style presets is subject to change.  |  null  |
| sampler | Sampler | STRING | TEXT  |  Which sampler to use for the diffusion process. If this value is omitted, an appropriate sampler will be automatically selected.  |  null  |
| seed | Seed | NUMBER | NUMBER  |  Random noise seed (omit this option or use 0 for a random seed). Valid range: 0 to 4294967295.  |  null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| url | STRING | TEXT  |
| b64Json | STRING | TEXT  |








