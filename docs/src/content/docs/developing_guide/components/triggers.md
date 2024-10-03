---
title: "Working with Triggers"
description: "Working with Triggers as a developer"
---

## Before you start testing Triggers
- download [ngrok](https://ngrok.com/download)
- open terminal and type `ngrok http http://127.0.0.1:9555`
- copy the first address under Forwarding
![ngork example](..%2F..%2F..%2F..%2F..%2F..%2F..%2F..%2FDownloads%2FUntitled%20%281%29.png)

### If you are on Intellij
- open bytechef code
- go to `bytechef/server/apps/server-app/src/main/resources/config` , create a file called `application-local.yml`  (Pay attention that `application-local.yml` is optional, git ignored file, and corresponds to local spring profile. This one shouldn’t miss activating `local` profile on springBoot startup)
- it is important to add local to active profiles in configuration
![Screenshot from 2024-07-09 10-48-58.png](..%2F..%2F..%2F..%2F..%2F..%2F..%2F..%2FDownloads%2FScreenshot%20from%202024-07-09%2010-48-58.png)

- paste this code in `application-local.yml`:
```
bytechef:
    webhook-url: (first address under Forwarding)/webhooks/{id}
```
- run Bytechef

### If you are not on Intellij
- paste this code in shell:
```
export BYTECHEF_WEBHOOK_URL=(first address under Forwarding)/webhooks/{id}
```
- run Bytechef
