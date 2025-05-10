---
title: "ElevenLabs"
description: "ElevenLabs is an AI-powered voice synthesis company specializing in ultra-realistic text-to-speech and voice cloning technology."
---

ElevenLabs is an AI-powered voice synthesis company specializing in ultra-realistic text-to-speech and voice cloning technology.


Categories: Artificial Intelligence


Type: elevenLabs/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | Key | STRING |  | true |
| value | API Key | STRING |  | true |





<hr />



## Actions


### Create Sound Effect
Name: createSoundEffect

Turn text into sound effects for your videos, voice-overs or video games using the most advanced sound effects model in the world.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| text | Text | STRING | The text that will get converted into a sound effect. | true |

#### Example JSON Structure
```json
{
  "label" : "Create Sound Effect",
  "name" : "createSoundEffect",
  "parameters" : {
    "text" : ""
  },
  "type" : "elevenLabs/v1/createSoundEffect"
}
```

#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.




### Create Speech
Name: createSpeech

Converts text into speech using a voice of your choice and returns audio.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| voiceId | Voice | STRING | Voice you want to use for converting the text into speech. | true |
| text | Text | STRING | Text you want to convert into speech. | true |

#### Example JSON Structure
```json
{
  "label" : "Create Speech",
  "name" : "createSpeech",
  "parameters" : {
    "voiceId" : "",
    "text" : ""
  },
  "type" : "elevenLabs/v1/createSpeech"
}
```

#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.




### Create Speech With Timing
Name: createSpeechWithTiming

Generate speech from text with precise character-level timing information for audio-text synchronization.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| voiceId | Voice | STRING | Voice you want to use for converting the text into speech. | true |
| text | Text | STRING | Text you want to convert into speech. | true |

#### Example JSON Structure
```json
{
  "label" : "Create Speech With Timing",
  "name" : "createSpeechWithTiming",
  "parameters" : {
    "voiceId" : "",
    "text" : ""
  },
  "type" : "elevenLabs/v1/createSpeechWithTiming"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| audio_base64 | STRING | Base64 encoded audio data |
| alignment | OBJECT <details> <summary> Properties </summary> {[STRING]\(characters), [NUMBER]\(character_start_times_seconds), [NUMBER]\(character_end_times_seconds)} </details> |  |
| normalized_alignment | OBJECT <details> <summary> Properties </summary> {[STRING]\(characters), [NUMBER]\(character_start_times_seconds), [NUMBER]\(character_end_times_seconds)} </details> |  |




#### Output Example
```json
{
  "audio_base64" : "",
  "alignment" : {
    "characters" : [ "" ],
    "character_start_times_seconds" : [ 0.0 ],
    "character_end_times_seconds" : [ 0.0 ]
  },
  "normalized_alignment" : {
    "characters" : [ "" ],
    "character_start_times_seconds" : [ 0.0 ],
    "character_end_times_seconds" : [ 0.0 ]
  }
}
```


### Create Transcript
Name: createTranscript

Transcribe an audio or video file.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| model_id | Model | STRING | The ID of the model to use for transcription, currently only ‘scribe_v1’ is available. | true |
| file | File | FILE_ENTRY | The file to transcribe. All major audio and video formats are supported. The file size must be less than 1GB. | true |

#### Example JSON Structure
```json
{
  "label" : "Create Transcript",
  "name" : "createTranscript",
  "parameters" : {
    "model_id" : "",
    "file" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    }
  },
  "type" : "elevenLabs/v1/createTranscript"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| language_code | STRING | The detected language code (e.g. ‘eng’ for English). |
| language_probability | NUMBER | The confidence score of the language detection (0 to 1). |
| text | STRING | The raw text of the transcription. |
| words | ARRAY <details> <summary> Items </summary> [{STRING\(text), NUMBER\(start), NUMBER\(end), STRING\(type)}] </details> | List of words with their timing information. |




#### Output Example
```json
{
  "language_code" : "",
  "language_probability" : 0.0,
  "text" : "",
  "words" : [ {
    "text" : "",
    "start" : 0.0,
    "end" : 0.0,
    "type" : ""
  } ]
}
```




<hr />

# Additional instructions
<hr />

