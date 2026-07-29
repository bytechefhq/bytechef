

# TaskExecutionModel

The execution of a single workflow task, including its input and output.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **String** | The id of a task execution. |  [optional] |
|**name** | **String** | The name of the executed workflow task. |  [optional] |
|**title** | **String** | The title of the executed workflow task. |  [optional] |
|**type** | **String** | The type of the executed workflow task. |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) | The status of a task execution. |  [optional] |
|**startDate** | **OffsetDateTime** | The instant a task execution started. |  [optional] |
|**endDate** | **OffsetDateTime** | The instant a task execution ended. |  [optional] |
|**error** | [**ExecutionErrorModel**](ExecutionErrorModel.md) |  |  [optional] |
|**input** | **Map&lt;String, Object&gt;** | The resolved input parameters of a task execution. |  [optional] |
|**output** | **Object** | The output of a task execution. |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| CREATED | &quot;CREATED&quot; |
| STARTED | &quot;STARTED&quot; |
| COMPLETED | &quot;COMPLETED&quot; |
| FAILED | &quot;FAILED&quot; |
| CANCELLED | &quot;CANCELLED&quot; |



