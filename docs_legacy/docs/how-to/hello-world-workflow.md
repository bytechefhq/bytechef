---
id: hello-world-workflow
title: Hello World Workflow Sample
sidebar_label: Hello World Workflow
---

# 1. Goal

Pass arbitrary text to workflow definition and get that text printed in log.

# 2. Requirements
- understand yaml syntax
- have running all-in-one docker instance
- have access to folder where workflow definition would be deployed

# 3. Implementation guide

- create yaml file hello-world.yaml
- copy/paste following content to the hello-world.yaml file:
  ```
  label: Hello World Workflow
  inputs:
    - name: practitionerName
      label: Practitioner Name
      type: string
      required: true
  tasks:
    - type: logger/v1/info
      name: printPractitionerName
      label: Print practitioner name in platform log
      text: "Hello ${practitionerName}!"
  ```
## 3.1 Workflow Definition 
Use leading `label` to provide human-readable workflow name. Declare `inputs` node to provide workflow with arbitrary list of input arguments. Note that `inputs` is optional and may be left out when use case doesn't require it.
Hello World Workflow requires input text to print it in platform log. Syntax to declare input argument is:
````
- name: practicionerName // defines input variable name one may use in workflow tasks
  label: Practitioner Name // defines human-readable name used by UI
  type: string // defines variable type
  required: true // tells the workflow engine argument is required
````
Number of input arguments is not limited. Modifiers `label` and `required` are not mandatory. If `label` is left out, name would be used as default value, and if `required` is not set, default value is `false`.

Execution flow is described in the `tasks` list. Task list entry mandatory fields are:
- `type` - tells engine component, version and operation (`component/version/operation`)
- `name` - sets unique name to the task and uses it to create variable with same name. Variable may contain result of this operation. It is accessible via `${taskName}` to all subsequent tasks and may be used later in expressions.   

Depending on the `type` modifier task may imply more mandatory fields. Description of task arguments may be found in component technical documentation. In our Hello World example component `logger/v1/info` is used. It expects one more required field `text` which defines expression to create log message task will print out in log. Use `${practitionerName}` expression within `text` string to customize log message.   
````
- type: logger/v1/info
  name: printPractitionerName
  label: Print practitioner name in platform log
  text: "Hello ${practitionerName}!"
```` 
Content of hello-world.yaml is ready for execution.
## 3.2 Run Workflow
Take hello-world.yaml and paste it to [BYTE_CHEF_WORKFLOW_DEPLOY_FOLDER]. From browser position to `http://localhost:9555/webjars/swagger-ui/4.14.0/index.html#/JobController/postJob` and provide this payload:

````
{
 "workflowId": "samples/liferay_object",
  "inputs": {
    "practitionerName":"Byte Chef User Guide"
  }
}
````
click execute and monitor main application terminal. Task is successfully executed if terminal contains log output: Hello Byte Chef User Guide!

## 3.3 References
A practitioner may refer to this and other completed workflow yaml samples in ByteChef samples folder [bytechef-samples](https://github.com/bytechefhq/bytechef/tree/master/server/apps/server-app/src/main/resources/workflows/samples/hello.yaml).