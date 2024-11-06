---
title: "Azure OpenAI"
description: "Azure OpenAI is a research organization that aims to develop and direct artificial intelligence (AI) in ways that benefit humanity as a whole."
---
## Reference
<hr />

Azure OpenAI is a research organization that aims to develop and direct artificial intelligence (AI) in ways that benefit humanity as a whole.


Categories: [artificial-intelligence]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Endpoint | STRING | TEXT  |  |
| Token | STRING | TEXT  |  |





<hr />





## Actions


### Ask
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | TEXT  |  Deployment name, written in string.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Response Format | INTEGER | SELECT  |  In which format do you want the response to be in?  |
| Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Number of Chat Completion Choices | INTEGER | INTEGER  |  How many chat completion choices to generate for each input message.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Frequency Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.  |
| Presence Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.  |
| Logit Bias | {} | OBJECT_BUILDER  |  Modify the likelihood of specified tokens appearing in the completion.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |
| Functions | [STRING] | ARRAY_BUILDER  |  Enter the names of functions you want to use.  |
| User | STRING | TEXT  |  A unique identifier representing your end-user, which can help admins to monitor and detect abuse.  |




### Create Image
Create an image using text-to-image models

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  The model to use for image generation.  |
| Messages | [{STRING\(content), NUMBER\(weight)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Size | {} | SELECT  |  The size of the generated images.  |
| Number of Responses | INTEGER | INTEGER  |  The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported..  |
| Response Format | STRING | SELECT  |  The format in which the generated images are returned.  |
| Style | STRING | SELECT  |  The style of the generated images. Must be one of vivid or natural. Vivid causes the model to lean towards generating hyper-real and dramatic images. Natural causes the model to produce more natural, less hyper-real looking images. This parameter is only supported for dall-e-3.  |
| User | STRING | TEXT  |  A unique identifier representing your end-user, which can help admins to monitor and detect abuse.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| [{STRING\(url), STRING\(b64Json), STRING\(revisedPrompt)}] | ARRAY_BUILDER  |






### Create Transcriptions
Transcribes audio into the input language.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| File | FILE_ENTRY | FILE_ENTRY  |  The audio file object to transcribe, in one of these formats: flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, or webm.  |
| Model | STRING | SELECT  |  ID of the model to use.  |
| Language | STRING | SELECT  |  The language of the input audio.  |
| Prompt | STRING | TEXT  |  An optional text to guide the model's style or continue a previous audio segment. The prompt should match the audio language.  |
| Response Format | {} | SELECT  |  The format of the transcript output  |
| Temperature | NUMBER | NUMBER  |  The sampling temperature, between 0 and 1. Higher values like will make the output more random, while lower values will make it more focused and deterministic.   |




