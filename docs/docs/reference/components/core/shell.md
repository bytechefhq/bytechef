---
id: shell
title: Shell
---

TODO

[shell/bashTaskHandler](src/main/java/com/ByteChef/atlas/task/handler/shell/Bash.java)

```
  name: listOfFiles
  type: shell/bashTaskHandler
  script: |
        for f in /tmp
        do
          echo "$f"
        done
```
