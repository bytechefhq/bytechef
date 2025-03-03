---
title: "Text Helper"
description: "Helper component which contains operations to help you work with text."
---

Helper component which contains operations to help you work with text.


Categories: helpers


Type: textHelper/v1

<hr />




## Actions


### Base64 Decode
Name: base64Decode

Decodes base64 encoded text into human readable plain text.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| encodingSchema | Encoding Scheme | STRING <details> <summary> Options </summary> base64, base64Url </details> |  | true |
| content | Base64 Content | STRING | The Base64 encoded content that needs to be decoded. | true |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "Base64 Decode",
  "name" : "base64Decode",
  "parameters" : {
    "encodingSchema" : "",
    "content" : ""
  },
  "type" : "textHelper/v1/base64Decode"
}
```


### Concatenate
Name: concatenate

Concatenate two or more texts.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| texts | Texts | ARRAY <details> <summary> Items </summary> [STRING\($text)] </details> |  | true |
| separator | Separator | STRING | The text that separates the texts you want to concatenate. | false |


#### Output



Type: STRING





#### JSON Example
```json
{
  "label" : "Concatenate",
  "name" : "concatenate",
  "parameters" : {
    "texts" : [ "" ],
    "separator" : ""
  },
  "type" : "textHelper/v1/concatenate"
}
```


### Extract Content from HTML
Name: extractContentFromHtml

Extract content from the HTML content.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| content | HTML Content | STRING | HTML content to extract content from. | true |
| querySelector | CSS Selector | STRING | The CSS selector to search for. | true |
| returnValue | Return Value | STRING <details> <summary> Options </summary> ATTRIBUTE, HTML, TEXT </details> | The data to return. | true |
| attribute | Attribute | STRING | The name of the attribute to return the value of | true |
| returnArray | Return Array | BOOLEAN <details> <summary> Options </summary> true, false </details> | If selected, then extracted individual items are returned as an array. If you don't set this, all values are returned as a single string. | null |


#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.

#### JSON Example
```json
{
  "label" : "Extract Content from HTML",
  "name" : "extractContentFromHtml",
  "parameters" : {
    "content" : "",
    "querySelector" : "",
    "returnValue" : "",
    "attribute" : "",
    "returnArray" : false
  },
  "type" : "textHelper/v1/extractContentFromHtml"
}
```


### Find
Name: find

Find substring

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| text | Text | STRING |  | true |
| expression | Expression | STRING | Text to search for. | true |


#### Output



Type: BOOLEAN





#### JSON Example
```json
{
  "label" : "Find",
  "name" : "find",
  "parameters" : {
    "text" : "",
    "expression" : ""
  },
  "type" : "textHelper/v1/find"
}
```


### HTML to Markdown
Name: HTMLToMarkdown

Converts HTML to markdown.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| html | HTML Content | STRING | HTML content to be converted to markdown. | true |


#### Output



Type: STRING





#### JSON Example
```json
{
  "label" : "HTML to Markdown",
  "name" : "HTMLToMarkdown",
  "parameters" : {
    "html" : ""
  },
  "type" : "textHelper/v1/HTMLToMarkdown"
}
```


### Markdown to HTML
Name: markdownToHTML

Converts markdown to HTML.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| markdown | Markdown content | STRING | Markdown content to convert to HTML. | true |


#### Output



Type: STRING





#### JSON Example
```json
{
  "label" : "Markdown to HTML",
  "name" : "markdownToHTML",
  "parameters" : {
    "markdown" : ""
  },
  "type" : "textHelper/v1/markdownToHTML"
}
```


### Replace
Name: replace

Replace all instances of any word, character, or phrase in text with another.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| text | Text | STRING |  | true |
| searchValue | Search Value | STRING | Can be plain text or a regex expression. | true |
| replaceValue | Replace Value | STRING | Leave blank to remove the search value. | false |
| replaceOnlyFirst | Replace Only First Match | BOOLEAN <details> <summary> Options </summary> true, false </details> |  | true |


#### Output



Type: STRING





#### JSON Example
```json
{
  "label" : "Replace",
  "name" : "replace",
  "parameters" : {
    "text" : "",
    "searchValue" : "",
    "replaceValue" : "",
    "replaceOnlyFirst" : false
  },
  "type" : "textHelper/v1/replace"
}
```


### Split
Name: split

Split the text by delimiter.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| text | Text | STRING |  | true |
| delimiter | Delimiter | STRING | Delimiter used for splitting the text. | true |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
|  | STRING |




#### JSON Example
```json
{
  "label" : "Split",
  "name" : "split",
  "parameters" : {
    "text" : "",
    "delimiter" : ""
  },
  "type" : "textHelper/v1/split"
}
```




