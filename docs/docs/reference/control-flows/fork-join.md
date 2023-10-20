---
id: fork-join
title: Fork/Join
---

TODO

Executes each branch in the `branches` as a separate and isolated sub-flow. Branches are executed internally in sequence.

```
- type: fork
  branches: 
     - - name: randomNumber                 <-- branch 1 start here
         label: Generate a random number
         type: random/int
         startInclusive: 0
         endInclusive: 5000
           
       - type: time/sleepTaskHandler
         millis: ${randomNumber}
           
     - - name: randomNumber                 <-- branch 2 start here
         label: Generate a random number
         type: random/int
         startInclusive: 0
         endInclusive: 5000
           
       - type: time/sleepTaskHandler
         millis: ${randomNumber}      
```
