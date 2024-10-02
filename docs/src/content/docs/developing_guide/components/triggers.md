---
title: "Working with Triggers"
description: "Working with Triggers as a developer"
---

### Before you start testing Triggers
- download [ngrok](https://ngrok.com/download)
- open terminal and type `ngrok http [http://127.0.0.1:9555](http://127.0.0.1:9555/)`
- copy the first address under Forwarding

- open bytechef code
- go to `bytechef/server/apps/server-app/src/main/resources/config` , create a file called `application-local.yml`  (Pay attention that `application-local.yml` is optional, git ignored file, and corresponds to local spring profile. This one shouldn’t miss activating `local` profile on springBoot startup)

- paste this code in shell:

```
export BYTECHEF_WEBHOOK_URL=(first address under Forwarding)/webhooks/{id}
```
- run bytechef
