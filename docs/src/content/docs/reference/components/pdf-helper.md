---
title: "PDF Helper"
description: "null"
---

null


Categories: Helpers


Type: pdfHelper/v1

<hr />




## Actions


### Extract Text
Name: extractText

Extracts text from a PDF file.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| file | PDF File | FILE_ENTRY | The PDF file from which to extract text. | true |

#### Example JSON Structure
```json
{
  "label" : "Extract Text",
  "name" : "extractText",
  "parameters" : {
    "file" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    }
  },
  "type" : "pdfHelper/v1/extractText"
}
```

#### Output



Type: STRING










