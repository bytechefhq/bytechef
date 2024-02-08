---
id: if
title: If
---

TODO

Executes true/false branch based on the `conditions` and `combineOperation` value.

```
- type: if
  conditions:
    - boolean:
        value1: true
        operation: notEquals
        value2: false
    - number:
        value1: 1
        operation: greater
        value2: 1
  combineOperation: any
  caseTrue:
    - type: io/print
      text: true branch
  caseFalse:
    - type: io/print
      text: false branch
```
