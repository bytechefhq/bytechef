---
id: parallel
title: Parallel
---

TODO

Run the `tasks` collection of functions in parallel, without waiting until the previous function has completed.

```
- type: parallel
  tasks: 
    - type: io/print
      text: hello
        
    - type: io/print
      text: goodbye
```
