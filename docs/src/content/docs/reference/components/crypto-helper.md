---
title: "Crypto Helper"
description: "The Crypto Helper allows you to use cryptographic functions."
---

The Crypto Helper allows you to use cryptographic functions.


Categories: helpers


Type: cryptoHelper/v1

<hr />




## Actions


### Generate Password
Name: generatePasswordAction

Generate a random password of the specified length.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| length | Length | INTEGER | The length of the password. | true |
| characterSet | Character Set | STRING <details> <summary> Options </summary> ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789, ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}\|;:'",<.>/? </details> | The character set to be used for generating the password. | true |

#### Example JSON Structure
```json
{
  "label" : "Generate Password",
  "name" : "generatePasswordAction",
  "parameters" : {
    "length" : 1,
    "characterSet" : ""
  },
  "type" : "cryptoHelper/v1/generatePasswordAction"
}
```

#### Output



Type: STRING








### Hash
Name: hashAction

Computes and returns the hash of the input.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| algorithm | Cryptographic Algorithm | STRING <details> <summary> Options </summary> MD5, SHA-1, SHA-256 </details> | The cryptographic algorithm that will be used to hash the input. | true |
| input | Input | STRING | Calculates the hash of the provided input. | true |

#### Example JSON Structure
```json
{
  "label" : "Hash",
  "name" : "hashAction",
  "parameters" : {
    "algorithm" : "",
    "input" : ""
  },
  "type" : "cryptoHelper/v1/hashAction"
}
```

#### Output



Type: STRING








### Hmac
Name: hmacAction

Computes and returns the HMAC of the input.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| algorithm | Cryptographic Algorithm | STRING <details> <summary> Options </summary> HmacMD5, HmacSHA1, HmacSHA256 </details> | The cryptographic algorithm that will be used to hash the input. | true |
| input | Input | STRING | Generates a cryptographic HMAC for the provided input. | true |
| key | Key | STRING | Key that will be used for the encryption. | true |

#### Example JSON Structure
```json
{
  "label" : "Hmac",
  "name" : "hmacAction",
  "parameters" : {
    "algorithm" : "",
    "input" : "",
    "key" : ""
  },
  "type" : "cryptoHelper/v1/hmacAction"
}
```

#### Output



Type: STRING










<hr />

# Additional instructions
<hr />
