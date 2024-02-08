---
id: map
title: Map
---

TODO

Produces a new collection of values by mapping each value in `list` through the `iteratee` function. The `iteratee` is called with an item from `list` in parallel. When the `iteratee` is finished executing on all items the `map` task will return a list of execution results in an order which corresponds to the order of the source `list`.

```
- name: fileSizes 
  type: map
  list: ["/path/to/file1.txt","/path/to/file2.txt","/path/to/file3.txt"]
  iteratee:
    type: io/filesize         
    file: ${item}
```
