---
title: "One Simple API"
description: "A toolbox with all the things you need to get your project to success:  Image resize and CDN, PDF and Screenshots generation, Currency Exchange and Discounts, Email Validation, QR codes, and much more!"
---

A toolbox with all the things you need to get your project to success:  Image resize and CDN, PDF and Screenshots generation, Currency Exchange and Discounts, Email Validation, QR codes, and much more!


Categories: developer-tools


Type: oneSimpleAPI/v1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| token | API Token | STRING | TEXT |  | true |





<hr />



## Actions


### Currency Converter
Name: currencyConverter

Convert currency from one to another.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| from_currency | From Currency | STRING <details> <summary> Options </summary> AED, AFN, ALL, AMD, ANG, AOA, ARS, AUD, AWG, AZN, BAM, BBD, BDT, BGN, BHD, BIF, BMD, BND, BOB, BRL, BSD, BTN, BWP, BYN, BZD, CAD, CDF, CHF, CLP, CNY, COP, CRC, CUC, CUP, CVE, CZK, DJF, DKK, DOP, DZD, EGP, ERN, ETB, EUR, FJD, FKP, FOK, GBP, GEL, GGP, GHS, GIP, GMD, GNF, GTQ, GYD, HKD, HNL, HRK, HTG, HUF, IDR, ILS, IMP, INR, IQD, IRR, ISK, JMD, JOD, JPY, KES, KGS, KHR, KID, KMF, KRW, KWD, KYD, KZT, LAK, LBP, LKR, LRD, LSL, LYD, MAD, MDL, MGA, MKD, MMK, MNT, MOP, MRU, MUR, MVR, MWK, MXN, MYR, MZN, NAD, NGN, NIO, NOK, NPR, NZD, OMR, PAB, PEN, PGK, PHP, PKR, PLN, PYG, QAR, RON, RSD, RUB, RWF, SAR, SBD, SCR, SDG, SEK, SGD, SHP, SLL, SOS, SRD, SSP, STN, SYP, SZL, THB, TJS, TMT, TND, TOP, TRY, TTD, TVD, TWD, TZS, UAH, UGX, USD, UYU, UZS, VES, VND, VUV, WST, XAF, XCD, XDR, XOF, XPF, YER, ZAR, ZMW </details> | SELECT | Currency from which you want to convert. | true |
| to_currency | To Currency | STRING <details> <summary> Options </summary> AED, AFN, ALL, AMD, ANG, AOA, ARS, AUD, AWG, AZN, BAM, BBD, BDT, BGN, BHD, BIF, BMD, BND, BOB, BRL, BSD, BTN, BWP, BYN, BZD, CAD, CDF, CHF, CLP, CNY, COP, CRC, CUC, CUP, CVE, CZK, DJF, DKK, DOP, DZD, EGP, ERN, ETB, EUR, FJD, FKP, FOK, GBP, GEL, GGP, GHS, GIP, GMD, GNF, GTQ, GYD, HKD, HNL, HRK, HTG, HUF, IDR, ILS, IMP, INR, IQD, IRR, ISK, JMD, JOD, JPY, KES, KGS, KHR, KID, KMF, KRW, KWD, KYD, KZT, LAK, LBP, LKR, LRD, LSL, LYD, MAD, MDL, MGA, MKD, MMK, MNT, MOP, MRU, MUR, MVR, MWK, MXN, MYR, MZN, NAD, NGN, NIO, NOK, NPR, NZD, OMR, PAB, PEN, PGK, PHP, PKR, PLN, PYG, QAR, RON, RSD, RUB, RWF, SAR, SBD, SCR, SDG, SEK, SGD, SHP, SLL, SOS, SRD, SSP, STN, SYP, SZL, THB, TJS, TMT, TND, TOP, TRY, TTD, TVD, TWD, TZS, UAH, UGX, USD, UYU, UZS, VES, VND, VUV, WST, XAF, XCD, XDR, XOF, XPF, YER, ZAR, ZMW </details> | SELECT | Currency to which you want to convert. | true |
| from_value | Value | NUMBER | NUMBER | Value to convert. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| from_currency | STRING | TEXT |
| from_value | STRING | TEXT |
| to_currency | STRING | TEXT |
| to_value | NUMBER | NUMBER |
| to_exchange_rate | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Currency Converter",
  "name" : "currencyConverter",
  "parameters" : {
    "from_currency" : "",
    "to_currency" : "",
    "from_value" : 0.0
  },
  "type" : "oneSimpleAPI/v1/currencyConverter"
}
```


### URL Shortener
Name: urlShortener

Shorten your desired URL

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| url | URL | STRING | TEXT | Place the URL you want to shorten | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| url | STRING | TEXT |
| single_use | STRING | TEXT |
| temporary_redirect | STRING | TEXT |
| forward_params | STRING | TEXT |
| short_url | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "URL Shortener",
  "name" : "urlShortener",
  "parameters" : {
    "url" : ""
  },
  "type" : "oneSimpleAPI/v1/urlShortener"
}
```


### Web Page Information
Name: webInformation

Get information about a certain webpage

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| url | URL | STRING | TEXT | Place the web page url you want to get info from | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| general | OBJECT <details> <summary> Properties </summary> {STRING\(title), STRING\(description), STRING\(canonical)} </details> | OBJECT_BUILDER |
| twitter | OBJECT <details> <summary> Properties </summary> {STRING\(site), STRING\(title), STRING\(description)} </details> | OBJECT_BUILDER |
| og | OBJECT <details> <summary> Properties </summary> {STRING\(title), STRING\(url), STRING\(image), STRING\(description), STRING\(type)} </details> | OBJECT_BUILDER |




#### JSON Example
```json
{
  "label" : "Web Page Information",
  "name" : "webInformation",
  "parameters" : {
    "url" : ""
  },
  "type" : "oneSimpleAPI/v1/webInformation"
}
```




