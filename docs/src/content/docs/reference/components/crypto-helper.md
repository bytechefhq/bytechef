---
title: "Crypto Helper"
description: "The Crypto Helper allows you to use cryptographic functions."
---

The Crypto Helper allows you to use cryptographic functions.


Categories: Helpers


Type: cryptoHelper/v1

<hr />




## Actions


### Hash
Name: hash

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
  "name" : "hash",
  "parameters" : {
    "algorithm" : "",
    "input" : ""
  },
  "type" : "cryptoHelper/v1/hash"
}
```

#### Output



Type: STRING








### Hmac
Name: hmac

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
  "name" : "hmac",
  "parameters" : {
    "algorithm" : "",
    "input" : "",
    "key" : ""
  },
  "type" : "cryptoHelper/v1/hmac"
}
```

#### Output



Type: STRING










<hr />

# Additional instructions
<hr />

