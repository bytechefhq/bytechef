---
id: each
title: Each
---

TODO

Applies the function `iteratee` to each item in `list`, in parallel. Note, that since this function applies iteratee to each item in parallel, there is no guarantee that the `iteratee` functions will complete in order.

```
- type: each
  list: [1000,2000,3000]
  iteratee:
    type: time/sleepTaskHandler         
    millis: ${item}
```

This will generate three parallel tasks, one for each item in the list, which will `sleepTaskHandler` for 1, 2 and 3 seconds respectively.
