---
title: "Encryption Helper"
description: "A secure PGP encryption and decryption service powered by Bouncy Castle, enabling users to encrypt, decrypt, and manage sensitive data with OpenPGP standards."
---

A secure PGP encryption and decryption service powered by Bouncy Castle, enabling users to encrypt, decrypt, and manage sensitive data with OpenPGP standards.


Categories: helpers


Type: encryptionHelper/v1

<hr />




## Actions


### Decrypt
Name: decrypt

Decrypts PGP encrypted file using private key and passphrase.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| privateKey | Private PGP Key | STRING | Private PGP key that will decrypt the file. | true |
| file | File | FILE_ENTRY | File that will be decrypted. | true |
| passphrase | Passphrase | STRING | Passphrase that was used for encryption. | true |

#### Example JSON Structure
```json
{
  "label" : "Decrypt",
  "name" : "decrypt",
  "parameters" : {
    "privateKey" : "",
    "file" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    },
    "passphrase" : ""
  },
  "type" : "encryptionHelper/v1/decrypt"
}
```

#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| extension | STRING |  |
| mimeType | STRING |  |
| name | STRING |  |
| url | STRING |  |




#### Output Example
```json
{
  "extension" : "",
  "mimeType" : "",
  "name" : "",
  "url" : ""
}
```


### Encrypt
Name: encrypt

PGP encrypts the file using public key.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| publicKey | Public PGP Key | STRING | Public PGP key of the recipient of the encrypted file. | true |
| file | File | FILE_ENTRY | File that will be encrypted. | true |

#### Example JSON Structure
```json
{
  "label" : "Encrypt",
  "name" : "encrypt",
  "parameters" : {
    "publicKey" : "",
    "file" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    }
  },
  "type" : "encryptionHelper/v1/encrypt"
}
```

#### Output



Type: FILE_ENTRY


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| extension | STRING |  |
| mimeType | STRING |  |
| name | STRING |  |
| url | STRING |  |




#### Output Example
```json
{
  "extension" : "",
  "mimeType" : "",
  "name" : "",
  "url" : ""
}
```




<hr />

# Additional instructions
<hr />

