---
title: "Text Helper"
description: "Helper component which contains operations to help you work with text."
---
## Reference
<hr />

Helper component which contains operations to help you work with text.


Categories: [helpers]


Version: 1

<hr />






## Actions


### Base64 Decode
Decodes base64 encoded text into human readable plain text.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Encoding Scheme | STRING | SELECT  |  |
| Base64 Content | STRING | TEXT_AREA  |  The Base64 encoded content that needs to be decoded.  |




### Concatenate
Concatenate two or more texts.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Texts | [STRING\($text)] | ARRAY_BUILDER  |  |
| Separator | STRING | TEXT  |  The text that separates the texts you want to concatenate.  |


### Output



Type: STRING







### Extract Content from HTML
Extract content from the HTML content.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| HTML Content | STRING | TEXT_AREA  |  HTML content to extract content from.  |
| CSS Selector | STRING | TEXT  |  The CSS selector to search for.  |
| Return Value | STRING | SELECT  |  The data to return.  |
| Attribute | STRING | TEXT  |  The name of the attribute to return the value of  |
| Return Array | BOOLEAN | SELECT  |  If selected, then extracted individual items are returned as an array. If you don't set this, all values are returned as a single string.  |




### Find
Find substring

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Text | STRING | TEXT_AREA  |  |
| Expression | STRING | TEXT  |  Text to search for.  |


### Output



Type: BOOLEAN







### HTML to Markdown
Converts HTML to markdown.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| HTML Content | STRING | TEXT_AREA  |  HTML content to be converted to markdown.  |


### Output



Type: STRING







### Markdown to HTML
Converts markdown to HTML.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Markdown content | STRING | TEXT_AREA  |  Markdown content to convert to HTML.  |


### Output



Type: STRING







### Replace
Replace all instances of any word, character, or phrase in text with another.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Text | STRING | TEXT_AREA  |  |
| Search Value | STRING | TEXT  |  Can be plain text or a regex expression.  |
| Replace Value | STRING | TEXT  |  Leave blank to remove the search value.  |
| Replace Only First Match | BOOLEAN | SELECT  |  |


### Output



Type: STRING







### Split
Split the text by delimiter.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Text | STRING | TEXT_AREA  |  |
| Delimiter | STRING | TEXT  |  Delimiter used for splitting the text.  |


### Output



Type: ARRAY


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |






