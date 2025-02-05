---
title: "RabbitMQ"
description: "RabbitMQ is an open-source message broker software that enables efficient communication between different systems, applications, and services. It supports multiple messaging protocols and facilitates a reliable and flexible messaging system."
---

RabbitMQ is an open-source message broker software that enables efficient communication between different systems, applications, and services. It supports multiple messaging protocols and facilitates a reliable and flexible messaging system.



Type: rabbitMQ/v1

<hr />



## Connections

Version: 1

null



<hr />



## Actions


### Send Message
Send a new RabbitMQ message.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| queue | STRING | TEXT  |
| message | {} | OBJECT_BUILDER  |






## Triggers


### New Message
Triggers on new RabbitMQ messages.

Type: LISTENER
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| queue | STRING | TEXT  |





<hr />

