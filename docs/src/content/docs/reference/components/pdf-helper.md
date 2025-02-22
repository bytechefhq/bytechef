---
title: "PDF Helper"
description: "null"
---

null


Categories: helpers


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


#### Output



Type: STRING





#### JSON Example
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




