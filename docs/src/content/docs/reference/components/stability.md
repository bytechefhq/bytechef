---
title: "Stability AI"
description: "Activating humanity's potential through generative AI. Open models in every modality, for everyone, everywhere."
---
## Reference
<hr />

Activating humanity's potential through generative AI. Open models in every modality, for everyone, everywhere.


Categories: [artificial-intelligence]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| API Key | STRING | TEXT  |  |





<hr />





## Actions


### Create Image
Create an image using text-to-image models

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | TEXT  |  The model to use for image generation.  |
| Messages | [{STRING\(content), NUMBER\(weight)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Height | INTEGER | INTEGER  |  Height of the image to generate, in pixels, in an increment divisible by 64. Engine-specific dimension validation applies.  |
| Width | INTEGER | INTEGER  |  Width of the image to generate, in pixels, in an increment divisible by 64. Engine-specific dimension validation applies.  |
| Number of Responses | INTEGER | INTEGER  |  The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported..  |
| Response format | STRING | SELECT  |  The format in which the generated images are returned.  |
| Style | STRING | SELECT  |  Pass in a style preset to guide the image model towards a particular style. This list of style presets is subject to change.  |
| Steps | INTEGER | INTEGER  |  Number of diffusion steps to run. Valid range: 10 to 50.  |
| CFG scale | NUMBER | NUMBER  |  The strictness level of the diffusion process adherence to the prompt text. Range: 0 to 35.  |
| Clip guidance preset | STRING | TEXT  |  Pass in a style preset to guide the image model towards a particular style. This list of style presets is subject to change.  |
| Sampler | STRING | TEXT  |  Which sampler to use for the diffusion process. If this value is omitted, an appropriate sampler will be automatically selected.  |
| Seed | NUMBER | NUMBER  |  Random noise seed (omit this option or use 0 for a random seed). Valid range: 0 to 4294967295.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |






