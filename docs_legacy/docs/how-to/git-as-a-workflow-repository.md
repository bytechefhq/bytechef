---
id: git-as-a-workflow-repository
title: Using Git as a workflow repository
sidebar_label: Git as a workflow repository
---

Rather than storing the workflows in your local file system or database, you can also use Git to store them for you. This has great advantages, not the least of which is workflow versioning, Pull Requests and everything else Git has to offer.

To enable Git as a workflow repository set the `ATLAS_WORKFLOW-REPOSITORY_GIT_ENABLED` environment variable to `true`. By default, ByteChef will use the samples repository [bytechef-workflows](https://github.com/bytechefhq/bytechef-workflows).

You can change it by using the `ATLAS_WORKFLOW-REPOSITORY_GIT_URL` and `ATLAS_WORKFLOW-REPOSITORY_GIT_SEARCH-PATHS` environment variables.
