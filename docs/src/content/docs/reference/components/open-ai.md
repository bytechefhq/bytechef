---
title: "OpenAI"
description: "OpenAI is a research organization that aims to develop and direct artificial intelligence (AI) in ways that benefit humanity as a whole."
---

OpenAI is a research organization that aims to develop and direct artificial intelligence (AI) in ways that benefit humanity as a whole.


Categories: artificial-intelligence


Type: openAi/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | Token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Ask
Name: ask

Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | SELECT  |  ID of the model to use.  |  true  |
| messages | Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| response | Response | {STRING\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |  false  |
| maxTokens | Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |  null  |
| n | Number of Chat Completion Choices | INTEGER | INTEGER  |  How many chat completion choices to generate for each input message.  |  null  |
| temperature | Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |  null  |
| topP | Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |  null  |
| frequencyPenalty | Frequency Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.  |  null  |
| presencePenalty | Presence Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.  |  null  |
| logitBias | Logit Bias | {} | OBJECT_BUILDER  |  Modify the likelihood of specified tokens appearing in the completion.  |  null  |
| stop | Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |  null  |
| user | User | STRING | TEXT  |  A unique identifier representing your end-user, which can help admins to monitor and detect abuse.  |  false  |




### Create Image
Name: createImage

Create an image using text-to-image models

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | SELECT  |  The model to use for image generation.  |  true  |
| imageMessages | Messages | [{STRING\(content), NUMBER\(weight)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| size | Size | STRING | SELECT  |  The size of the generated images.  |  true  |
| n | Number of Responses | INTEGER | INTEGER  |  The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported..  |  null  |
| responseFormat | Response format | STRING | SELECT  |  The format in which the generated images are returned.  |  null  |
| quality | Quality | STRING | SELECT  |  The quality of the image that will be generated.  |  null  |
| style | Style | STRING | SELECT  |  The style of the generated images. Must be one of vivid or natural. Vivid causes the model to lean towards generating hyper-real and dramatic images. Natural causes the model to produce more natural, less hyper-real looking images. This parameter is only supported for dall-e-3.  |  null  |
| user | User | STRING | TEXT  |  A unique identifier representing your end-user, which can help admins to monitor and detect abuse.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| url | STRING | TEXT  |
| b64Json | STRING | TEXT  |






### Text-To-Speech
Name: createSpeech

Generate an audio recording from the input text

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | SELECT  |  Text-to-Speech model which will generate the audio.  |  true  |
| input | Input | STRING | TEXT  |  The text to generate audio for.  |  true  |
| voice | Voice | STRING | SELECT  |  The voice to use when generating the audio.  |  true  |
| responseFormat | Response format | STRING | SELECT  |  The format to audio in.  |  false  |
| speed | Speed | NUMBER | NUMBER  |  The speed of the generated audio.  |  false  |


#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| extension | STRING | TEXT  |
| mimeType | STRING | TEXT  |
| name | STRING | TEXT  |
| url | STRING | TEXT  |






### Create Transcriptions
Name: createTranscription

Transcribes audio into the input language.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| file | File | FILE_ENTRY | FILE_ENTRY  |  The audio file object to transcribe, in one of these formats: flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, or webm.  |  true  |
| model | Model | STRING | SELECT  |  ID of the model to use.  |  true  |
| language | Language | STRING | SELECT  |  The language of the input audio.  |  false  |
| prompt | Prompt | STRING | TEXT  |  An optional text to guide the model's style or continue a previous audio segment. The prompt should match the audio language.  |  false  |
| responseFormat | Response format | STRING | SELECT  |  The format of the transcript output  |  true  |
| temperature | Temperature | NUMBER | NUMBER  |  The sampling temperature, between 0 and 1. Higher values like will make the output more random, while lower values will make it more focused and deterministic.   |  false  |






<hr />

# Additional instructions
<hr />

## Connection Setup

1. Go to the [OpenAI API](https://platform.openai.com/settings/organization/general).
2. In the left sidebar, click on **API Keys**.
3. Click **Create new secret key**.
4. Enter a name and select the project for the key and click **Create secret key**.
5. Copy the API key and use it to create a connection in ByteChef.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(50.05219207% + 32px)">
<iframe src="https://www.guidejar.com/embed/coPV2tDXMJmcpVHubS0J?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>
