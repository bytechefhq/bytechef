---
title: "Apollo"
description: "Apollo.io is a sales intelligence and engagement platform that provides tools for prospecting, lead generation, and sales automation to help businesses improve their sales processes and outreach efforts."
---

Apollo.io is a sales intelligence and engagement platform that provides tools for prospecting, lead generation, and sales automation to help businesses improve their sales processes and outreach efforts.


Categories: crm


Type: apollo/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | Key | STRING |  | true |
| value | Value | STRING |  | true |



### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| clientId | Client Id | STRING |  | true |
| clientSecret | Client Secret | STRING |  | true |





<hr />



## Actions


### Update Deal
Name: updateDeal

Updates the details of existing deals within your team's Apollo account.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| opportunity_id | Opportunity Id | STRING | The ID for the deal you want to update. | true |
| owner_id | Owner ID | STRING | The ID for the deal owner within your team's Apollo account. | false |
| name | Name | STRING | New name for the deal. | false |
| closed_date | Close Date | DATE | Updated estimated close date for the deal. This can be a future or past date. | false |
| account_id | Account ID | STRING | The ID for the account within your Apollo instance. This is the company that you are targeting as part of the deal being created. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| opportunity | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(team_id), STRING\(owner_id), NUMBER\(amount), DATE\(closed_date), STRING\(account_id), STRING\(description), STRING\(name), {STRING\(name), STRING\(iso_code), STRING\(symbol)}\(currency)} </details> |




#### JSON Example
```json
{
  "label" : "Update Deal",
  "name" : "updateDeal",
  "parameters" : {
    "opportunity_id" : "",
    "owner_id" : "",
    "name" : "",
    "closed_date" : "2021-01-01",
    "account_id" : ""
  },
  "type" : "apollo/v1/updateDeal"
}
```


### Create Deal
Name: createDeal

Creates new deal for an Apollo account.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| name | Name | STRING | Name the deal you are creating. | true |
| owner_id | Owner ID | STRING | The ID for the deal owner within your team's Apollo account. | false |
| account_id | Account ID | STRING | The ID for the account within your Apollo instance. This is the company that you are targeting as part of the deal being created. | false |
| amount | Amount | STRING | The monetary value of the deal being created. Do not enter commas or currency symbols for the value.  | false |
| closed_date | Close Date | DATE | The estimated close date for the deal. This can be a future or past date. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| opportunity | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(team_id), STRING\(owner_id), NUMBER\(amount), DATE\(closed_date), STRING\(account_id), STRING\(description), STRING\(name), {STRING\(name), STRING\(iso_code), STRING\(symbol)}\(currency)} </details> |




#### JSON Example
```json
{
  "label" : "Create Deal",
  "name" : "createDeal",
  "parameters" : {
    "name" : "",
    "owner_id" : "",
    "account_id" : "",
    "amount" : "",
    "closed_date" : "2021-01-01"
  },
  "type" : "apollo/v1/createDeal"
}
```


### Enrich Person
Name: enrichPerson

Enriches data for a person.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| first_name | First Name | STRING | The first name of the person. | false |
| last_name | Last Name | STRING | The lst name of the person. | false |
| name | Name | STRING | The full name of the person. | false |
| email | Email | STRING | The email address of the person. | false |
| organization_name | Organization Name | STRING | The name of the person's employer. | false |
| domain | Domain | STRING | The domain name for the person's employer. This can be the current employer or a previous employer. Do not include www., the @ symbol, or similar. | false |
| linkedin_url | LinkedIn URL | STRING | The URL for the person's LinkedIn profile. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| person | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(first_name), STRING\(last_name), STRING\(name), STRING\(linkedin_url), STRING\(title), STRING\(email_status), STRING\(photo_url), STRING\(twitter_url), STRING\(github_url), STRING\(facebook_url), STRING\(headline), STRING\(email), STRING\(organization_id)} </details> |




#### JSON Example
```json
{
  "label" : "Enrich Person",
  "name" : "enrichPerson",
  "parameters" : {
    "first_name" : "",
    "last_name" : "",
    "name" : "",
    "email" : "",
    "organization_name" : "",
    "domain" : "",
    "linkedin_url" : ""
  },
  "type" : "apollo/v1/enrichPerson"
}
```


### Enrich Company
Name: enrichCompany

Enriches data for company.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| domain | Domain | STRING | The domain of the company that you want to enrich. Do not include www., the @ symbol, or similar. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| organization | OBJECT <details> <summary> Properties </summary> {STRING\(id), STRING\(name), STRING\(website_url), STRING\(blog_url), STRING\(linkedin_url), STRING\(twitter_url), STRING\(facebook_url), STRING\(phone), STRING\(logo_url), STRING\(primary_domain), STRING\(industry), [STRING]\(keywords)} </details> |




#### JSON Example
```json
{
  "label" : "Enrich Company",
  "name" : "enrichCompany",
  "parameters" : {
    "domain" : ""
  },
  "type" : "apollo/v1/enrichCompany"
}
```




