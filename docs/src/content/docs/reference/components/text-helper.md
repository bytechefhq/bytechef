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


### Extract Content from HTML
Extract content from the HTML content.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| HTML content to extract content from. | STRING | TEXT_AREA  |  The HTML content.  |
| CSS Selector | STRING | TEXT  |  The CSS selector to search for.  |
| Return Value | STRING | SELECT  |  The data to return.  |
| Attribute | STRING | TEXT  |  The name of the attribute to return the value of  |
| Return Array | BOOLEAN | SELECT  |  If selected, then extracted individual items are returned as an array. If you don't set this, all values are returned as a single string.  |




### Base64 Decode
Decodes base64 encoded text into human readable plain text.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Encoding Scheme | STRING | SELECT  |  |
| Base64 Content | STRING | TEXT_AREA  |  The Base64 encoded content that needs to be decoded.  |




