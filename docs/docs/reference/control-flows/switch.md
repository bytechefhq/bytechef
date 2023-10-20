---
id: switch
title: Switch
---

TODO

Executes one and only one branch of execution based on the `expression` value.

```
- type: switch
  expression: ${selector} <-- determines which case will be executed
  cases: 
     - key: hello                 <-- case 1 start here
       tasks: 
         - type: io/print
           text: hello world
     - key: bye                   <-- case 2 start here
       tasks: 
         - type: io/print
           text: goodbye world
  default:
    - tasks:
        -type: io/print
         text: something else
```
