---
id: subflow
title: Subflow
---

TODO

Starts a new job as a sub-flow of the current job. Output of the sub-flow job is the output of the task.

```    
- type: subflow
  workflowId: copy_files
  inputs: 
    - source: /path/to/source/dir
    - destination: /path/to/destination/dir
```
