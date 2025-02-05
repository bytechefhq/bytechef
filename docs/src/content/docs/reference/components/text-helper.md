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
Decodes base64 encoded text into human readable plain text.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| encodingSchema | Encoding Scheme | STRING | SELECT  |  | null  |
| content | Base64 Content | STRING | TEXT_AREA  |  The Base64 encoded content that needs to be decoded.  |  true  |




### Concatenate
Concatenate two or more texts.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| texts | Texts | [STRING\($text)] | ARRAY_BUILDER  |  | true  |
| separator | Separator | STRING | TEXT  |  The text that separates the texts you want to concatenate.  |  false  |


#### Output



Type: STRING







### Extract Content from HTML
Extract content from the HTML content.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| content | HTML Content | STRING | TEXT_AREA  |  HTML content to extract content from.  |  true  |
| querySelector | CSS Selector | STRING | TEXT  |  The CSS selector to search for.  |  true  |
| returnValue | Return Value | STRING | SELECT  |  The data to return.  |  true  |
| attribute | Attribute | STRING | TEXT  |  The name of the attribute to return the value of  |  true  |
| returnArray | Return Array | BOOLEAN | SELECT  |  If selected, then extracted individual items are returned as an array. If you don't set this, all values are returned as a single string.  |  null  |




### Find
Find substring

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| text | Text | STRING | TEXT_AREA  |  | true  |
| expression | Expression | STRING | TEXT  |  Text to search for.  |  true  |


#### Output



Type: BOOLEAN







### HTML to Markdown
Converts HTML to markdown.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| html | HTML Content | STRING | TEXT_AREA  |  HTML content to be converted to markdown.  |  true  |


#### Output



Type: STRING







### Markdown to HTML
Converts markdown to HTML.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| markdown | Markdown content | STRING | TEXT_AREA  |  Markdown content to convert to HTML.  |  true  |


#### Output



Type: STRING







### Replace
Replace all instances of any word, character, or phrase in text with another.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| text | Text | STRING | TEXT_AREA  |  | true  |
| searchValue | Search Value | STRING | TEXT  |  Can be plain text or a regex expression.  |  true  |
| replaceValue | Replace Value | STRING | TEXT  |  Leave blank to remove the search value.  |  false  |
| replaceOnlyFirst | Replace Only First Match | BOOLEAN | SELECT  |  | true  |


#### Output



Type: STRING







### Split
Split the text by delimiter.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| text | Text | STRING | TEXT_AREA  |  | true  |
| delimiter | Delimiter | STRING | TEXT  |  Delimiter used for splitting the text.  |  true  |


#### Output



Type: ARRAY


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
|  | STRING | TEXT  |








