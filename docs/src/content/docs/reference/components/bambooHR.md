---
title: "BambooHR"
description: "BambooHR is a Human Resources software that helps HR teams manage employee data, hiring, onboarding, time tracking, payroll, performance management, and more in one platform."
---

BambooHR is a Human Resources software that helps HR teams manage employee data, hiring, onboarding, time tracking, payroll, performance management, and more in one platform.


Categories: crm


Type: bambooHR/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| companyDomain | Company Domain | STRING | Text before .bamboohr.com when logged in to BambooHR. | true |
| username | API key | STRING |  | true |





<hr />



## Actions


### Create Employee
Name: createEmployee

Add a new employee.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| employeeNumber | Employee Number | STRING | The employee number of the employee. | true |
| firstName | First Name | STRING | The first name of the employee. | true |
| lastName | Last Name | STRING | The last name of the employee. | true |
| jobTitle | Job Title | STRING | The job title of the employee. | false |
| location | Location | STRING | The employee's current location. | false |
| employmentHistoryStatus | Employee Status | STRING | The employment status of the employee. | false |
| hireDate | Hire Date | DATE | The date the employee was hired. | false |

#### Example JSON Structure
```json
{
  "label" : "Create Employee",
  "name" : "createEmployee",
  "parameters" : {
    "employeeNumber" : "",
    "firstName" : "",
    "lastName" : "",
    "jobTitle" : "",
    "location" : "",
    "employmentHistoryStatus" : "",
    "hireDate" : "2021-01-01"
  },
  "type" : "bambooHR/v1/createEmployee"
}
```

#### Output

This action does not produce any output.




### Update Employee
Name: updateEmployee

Update an employee, based on employee ID.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Employee ID | STRING | The ID of the employee. | true |
| firstName | Updated First Name | STRING | The updated first name of the employee. | false |
| lastName | Updated Last Name | STRING | The updated last name of the employee. | false |
| jobTitle | Updated Job Title | STRING | The updated job title of the employee. | false |
| location | Updated Location | STRING | The updated employee's current location. | false |
| employmentHistoryStatus | Updated Employee Status | STRING | The updated employment status of the employee. | false |
| hireDate | Updated Hire Date | DATE | The updated date the employee was hired. | false |

#### Example JSON Structure
```json
{
  "label" : "Update Employee",
  "name" : "updateEmployee",
  "parameters" : {
    "id" : "",
    "firstName" : "",
    "lastName" : "",
    "jobTitle" : "",
    "location" : "",
    "employmentHistoryStatus" : "",
    "hireDate" : "2021-01-01"
  },
  "type" : "bambooHR/v1/updateEmployee"
}
```

#### Output

This action does not produce any output.




### Get Employee
Name: getEmployee

Get employee data, based on employee ID.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Employee ID | STRING | The ID of the employee. | true |
| fields | null | ARRAY <details> <summary> Items </summary> [STRING] </details> | Fields you want to get from employee. See documentation for available fields. | true |

#### Example JSON Structure
```json
{
  "label" : "Get Employee",
  "name" : "getEmployee",
  "parameters" : {
    "id" : "",
    "fields" : [ "" ]
  },
  "type" : "bambooHR/v1/getEmployee"
}
```

#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.




### Update Employee File
Name: updateEmployeeFile

Update an employee file.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| id | Employee ID | STRING | The ID of the employee. | true |
| fileId | File ID | STRING | The ID of the employee file being updated. | true |
| name | Updated Name Of The File | STRING | Use if you want to rename the file. | false |
| categoryId | Updated Category ID | STRING | Use if you want to move the file to a different category. | false |
| shareWithEmployee | Update Sharing The File | BOOLEAN <details> <summary> Options </summary> true, false </details> | Use if you want to update whether this file is shared or not. | false |

#### Example JSON Structure
```json
{
  "label" : "Update Employee File",
  "name" : "updateEmployeeFile",
  "parameters" : {
    "id" : "",
    "fileId" : "",
    "name" : "",
    "categoryId" : "",
    "shareWithEmployee" : false
  },
  "type" : "bambooHR/v1/updateEmployeeFile"
}
```

#### Output

This action does not produce any output.






