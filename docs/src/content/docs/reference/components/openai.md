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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Token | STRING | TEXT  |  |





<hr />





## Actions


### Ask ChatGPT
Ask ChatGPT anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Messages | [{STRING(content), STRING(role), STRING(name)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Model | STRING | SELECT  |  ID of the model to use.  |
| Frequency penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.  |
| Logit bias | {} | OBJECT_BUILDER  |  Modify the likelihood of specified tokens appearing in the completion.  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Number of chat completion choices | INTEGER | INTEGER  |  How many chat completion choices to generate for each input message.  |
| Presence penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top p | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| User | STRING | TEXT  |  A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(name), {}(arguments)} | OBJECT_BUILDER  |





### Create assistant
Create an assistant with a model and instructions.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  ID of the model to use.  |
| Name | STRING | TEXT  |  The name of the assistant.  |
| Description | STRING | TEXT  |  The description of the assistant.  |
| Instructions | STRING | TEXT  |  The system instructions that the assistant uses.  |
| Tools | [{STRING(type), {STRING(description), STRING(name), {}(parameters)}(function)}] | ARRAY_BUILDER  |  A list of tool enabled on the assistant.  |
| File ids | [STRING] | ARRAY_BUILDER  |  A list of file IDs attached to this assistant.  |
| Metadata | {} | OBJECT_BUILDER  |  Set of 16 key-value pairs that can be attached to an object. This can be useful for storing additional information about the object in a structured format. Keys can be a maximum of 64 characters long and values can be a maxium of 512 characters long.  |


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
| [{STRING(type), {STRING(description), STRING(name), {}(parameters)}(function)}] | ARRAY_BUILDER  |
| [STRING($fileId)] | ARRAY_BUILDER  |
| {} | OBJECT_BUILDER  |





### Create image
Create an image using text-to-image models

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| DYNAMIC_PROPERTIES | null  |
| Model | STRING | SELECT  |  The model to use for image generation.  |
| Quality | STRING | SELECT  |  The quality of the image that will be generated.  |
| Response format | STRING | SELECT  |  The format in which the generated images are returned.  |
| Size | STRING | SELECT  |  The size of the generated images.  |
| Style | STRING | SELECT  |  The style of the generated images.  |
| User | STRING | TEXT  |  A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| [{STRING(url), STRING(b64Json), STRING(revisedPrompt)}] | ARRAY_BUILDER  |





### Create speech
Generate an audio recording from the input text

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  Text-to-Speech model which will generate the audio.  |
| Input | STRING | TEXT  |  The text to generate audio for.  |
| Voice | STRING | SELECT  |  The voice to use when generating the audio.  |
| Response format | STRING | SELECT  |  The format to audio in.  |
| Speed | NUMBER | NUMBER  |  The speed of the generated audio.  |


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

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |  The audio file object to transcribe, in one of these formats: flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, or webm.  |
| Model | STRING | SELECT  |  ID of the model to use.  |
| Language | STRING | SELECT  |  The language of the input audio.  |
| Prompt | STRING | TEXT  |  An optional text to guide the model's style or continue a previous audio segment. The prompt should match the audio language.  |
| Response format | STRING | SELECT  |  The format of the transcript output  |
| Temperature | NUMBER | NUMBER  |  The sampling temperature, between 0 and 1. Higher values like will make the output more random, while lower values will make it more focused and deterministic.   |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| [{INTEGER(id), INTEGER(seek), NUMBER(start), NUMBER(end), STRING(text), [INTEGER](tokens), NUMBER(temperature), NUMBER(averageLogProb), NUMBER(compressionRatio), NUMBER(noSpeechProb), BOOLEAN(transientFlag)}] | ARRAY_BUILDER  |





### Create translation
Translates audio into English.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |  The audio file object translate, in one of these formats: flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, or webm.  |
| Model | STRING | SELECT  |  ID of the model to use.  |
| Prompt | STRING | TEXT  |  An optional text to guide the model's style or continue a previous audio segment. The prompt should be in English.  |
| Response format | STRING | SELECT  |  The format of the transcript output  |
| Temperature | NUMBER | NUMBER  |  The sampling temperature, between 0 and 1. Higher values like will make the output more random, while lower values will make it more focused and deterministic.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| NUMBER | NUMBER  |
| [{INTEGER(id), INTEGER(seek), NUMBER(start), NUMBER(end), STRING(text), [INTEGER($token)](tokens), NUMBER(temperature), NUMBER(averageLogProb), NUMBER(compressionRatio), NUMBER(noSpeechProb), BOOLEAN(transientFlag)}] | ARRAY_BUILDER  |





### Vision Prompt
Ask GPT a question about an image

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Messages | [{[{STRING(type), {STRING(url), STRING(detail)}(imageUrl)}](content), STRING(role), STRING(name)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Model | STRING | SELECT  |  ID of the model to use.  |
| Frequency penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.  |
| Logit bias | {} | OBJECT_BUILDER  |  Modify the likelihood of specified tokens appearing in the completion.  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Number of chat completion choices | INTEGER | INTEGER  |  How many chat completion choices to generate for each input message.  |
| Presence penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top p | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| User | STRING | TEXT  |  A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING(name), {}(arguments)} | OBJECT_BUILDER  |





