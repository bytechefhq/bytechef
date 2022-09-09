---
id: expression-functions
title: Expression Function Reference
---

ByteChef support the following constructs that expose various functions that can be used inside workflows:

## boolean

```
  type: core/var
  value: "${boolean('false')}"
```

## byte

```
  type: core/var
  value: "${byte('42')}"
```
## char

```
  type: core/var
  value: "${char('1')}"
```

## short

```
  type: core/var
  value: "${short('42')}"
```

# int

```
  type: core/var
  value: "${int('42')}"
```

## long

```
  type: core/var
  value: "${long('42')}"
```

## float

```
  type: core/var
  value: "${float('4.2')}"
```

## double

```
  type: core/var
  value: "${float('4.2')}"
```

## systemProperty

```
  type: core/var
  value: "${systemProperty('java.home')}"
```

## range

```
  type: core/var
  value: "${range(0,100)}" # [0,1,...,100]
```

## join

```
  type: core/var
  value: "${join('A','B','C')}" # ABC
```

## concat

```
  type: core/var
  value: ${concat(['A','B'],['C'])} # ['A','B','C']
```

## flatten

```
  type: core/var
  value: ${flatten([['A'],['B']])} # ['A','B']
```

## sort

```
  type: core/var
  value: ${sort([3,1,2])} # [1,2,3]
```

## tempDir

```
  type: core/var
  value: "${tempDir()}"  # e.g. /tmp
```

## uuid

```
  name: workDir
  type: core/var
  value: "${tempDir()}/${uuid()}"
```

## stringf

```
  type: core/var
  value: "${stringf('%03d',5)}"  # 005
```

## now

```
  type: core/varcus
  value: "${dateFormat(now(),'yyyy')}"  # e.g. 2020
```

## timestamp

```
  type: core/var
  value: "${timestamp()}"  # e.g. 1583268621423
```

## dateFormat

```
  type: core/var
  value: "${dateFormat(now(),'yyyy')}"  # e.g. 2020
```

## config

```
  type: core/var
  value: "${config('some.config.property')}"
```
