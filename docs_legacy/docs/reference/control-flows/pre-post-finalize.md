---
id: pre-post-finalize
title: Pre/Post/Finalize
---

Each task can define a set of tasks that will be executed prior to its execution (`pre`),
after its succesful execution (`post`) and at the end of the task's lifecycle regardless of the outcome of the task's
execution (`finalize`).

`pre/post/finalize` tasks always execute on the same node which will execute the task itself and are considered to be an atomic part of the task. That is, failure in any of the `pre/post/finalize` tasks is considered a failure of the entire task.


```
  - label: 240p
    type: media/ffmpeg
    options: [
      "-y",
      "-i",
      "/some/input/video.mov",
      "-vf","scale=w=-2:h=240",
      "${workDir}/240p.mp4"
    ]
    pre:
      - name: workDir
        type: core/var
        value: "${temptDir()}/${uuid()}"
      - type: io/mkdirOperation
        path: "${workDir}"
    post: 
      - type: s3/putObject
        uri: s3://my-bucket/240p.mp4
    finalize:
      - type: io/rmOperation
        path: ${workDir}
```
