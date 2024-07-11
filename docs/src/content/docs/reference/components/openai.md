---
title: "OpenAI"
description: "OpenAI is a research organization that aims to develop and direct artificial intelligence (AI) in ways that benefit humanity as a whole."
---
## Reference
<hr />

OpenAI is a research organization that aims to develop and direct artificial intelligence (AI) in ways that benefit humanity as a whole.

Categories: [ARTIFICIAL_INTELLIGENCE]

Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Token | STRING | TEXT  |





<hr />





## Actions


### Ask ChatGPT
Ask ChatGPT anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Messages | ARRAY | ARRAY_BUILDER  |
| Model | STRING | SELECT  |
| Frequency penalty | NUMBER | NUMBER  |
| Logit bias | OBJECT | OBJECT_BUILDER  |
| Max tokens | INTEGER | INTEGER  |
| Number of chat completion choices | INTEGER | INTEGER  |
| Presence penalty | NUMBER | NUMBER  |
| Stop | ARRAY | ARRAY_BUILDER  |
| Temperature | NUMBER | NUMBER  |
| Top p | NUMBER | NUMBER  |
| User | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





### Create assistant
Create an assistant with a model and instructions.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Model | STRING | SELECT  |
| Name | STRING | TEXT  |
| Description | STRING | TEXT  |
| Instructions | STRING | TEXT  |
| Tools | ARRAY | ARRAY_BUILDER  |
| File ids | ARRAY | ARRAY_BUILDER  |
| Metadata | OBJECT | OBJECT_BUILDER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| INTEGER | INTEGER  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| ARRAY | ARRAY_BUILDER  |
| ARRAY | ARRAY_BUILDER  |
| OBJECT | OBJECT_BUILDER  |





### Create image
Create an image using text-to-image models

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| DYNAMIC_PROPERTIES | null  |
| Model | STRING | SELECT  |
| Quality | STRING | SELECT  |
| Response format | STRING | SELECT  |
| Size | STRING | SELECT  |
| Style | STRING | SELECT  |
| User | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| ARRAY | ARRAY_BUILDER  |





### Create speech
Generate an audio recording from the input text

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Model | STRING | SELECT  |
| Input | STRING | TEXT  |
| Voice | STRING | SELECT  |
| Response format | STRING | SELECT  |
| Speed | NUMBER | NUMBER  |


### Output



Type: FILE_ENTRY

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |





### Create transcriptions
Transcribes audio into the input language.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |
| Model | STRING | SELECT  |
| Language | STRING | SELECT  |
| Prompt | STRING | TEXT  |
| Response format | STRING | SELECT  |
| Temperature | NUMBER | NUMBER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| ARRAY | ARRAY_BUILDER  |





### Create translation
Translates audio into English.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |
| Model | STRING | SELECT  |
| Prompt | STRING | TEXT  |
| Response format | STRING | SELECT  |
| Temperature | NUMBER | NUMBER  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| ARRAY | ARRAY_BUILDER  |





### Vision Prompt
Ask GPT a question about an image

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Messages | ARRAY | ARRAY_BUILDER  |
| Model | STRING | SELECT  |
| Frequency penalty | NUMBER | NUMBER  |
| Logit bias | OBJECT | OBJECT_BUILDER  |
| Max tokens | INTEGER | INTEGER  |
| Number of chat completion choices | INTEGER | INTEGER  |
| Presence penalty | NUMBER | NUMBER  |
| Stop | ARRAY | ARRAY_BUILDER  |
| Temperature | NUMBER | NUMBER  |
| Top p | NUMBER | NUMBER  |
| User | STRING | TEXT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| OBJECT | OBJECT_BUILDER  |





