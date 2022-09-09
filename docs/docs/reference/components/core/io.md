---
id: io
title: IO
---

TODO

[io/createTempDir](src/main/java/com/ByteChef/atlas/task/handler/io/CreateTempDir.java)

```
  name: tempDir
  type: io/create-temp-dir
```

[io/filepath](src/main/java/com/ByteChef/atlas/task/handler/io/FilePath.java)

```
  name: myFilePath
  type: io/filepath
  filename: /path/to/my/file.txt
```

[io/lsOperation](src/main/java/com/ByteChef/atlas/task/handler/io/Ls.java)

```
  name: listOfFiles
  type: io/lsOperation
  recursive: true # default: false
  path: /path/to/directory
```

[io/mkdirOperation](src/main/java/com/ByteChef/atlas/task/handler/io/Mkdir.java)

```
  type: io/mkdirOperation
  path: /path/to/directory
```

[io/print](src/main/java/com/ByteChef/atlas/task/handler/io/Print.java)

```
  type: io/print
  text: hello world
```

[io/rmOperation](src/main/java/com/ByteChef/atlas/task/handler/io/Rm.java)

```
  type: io/rmOperation
  path: /some/directory
```
