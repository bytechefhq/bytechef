

# AutomationWorkflowProjectModel

An automation workflow catalog project.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**name** | **String** | The name of the automation workflow project. |  [optional] |
|**kind** | [**KindEnum**](#KindEnum) | Whether copying this project&#39;s templates creates a per-user copy or a shared reference. |  [optional] |
|**workflowTemplates** | [**List&lt;AutomationWorkflowProjectWorkflowTemplateModel&gt;**](AutomationWorkflowProjectWorkflowTemplateModel.md) | The list of catalog workflow templates belonging to this project. |  [optional] |



## Enum: KindEnum

| Name | Value |
|---- | -----|
| COPY | &quot;COPY&quot; |
| REFERENCE | &quot;REFERENCE&quot; |



