

# WorkflowExecutionModel

The full detail of a workflow execution, including inputs, outputs, error and task executions.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **Long** | The id of a workflow execution. |  [readonly] |
|**status** | **WorkflowExecutionStatusModel** |  |  [optional] |
|**startDate** | **OffsetDateTime** | The instant an execution started. |  [optional] |
|**endDate** | **OffsetDateTime** | The instant an execution ended. Null while the execution is running. |  [optional] |
|**error** | [**ExecutionErrorModel**](ExecutionErrorModel.md) |  |  [optional] |
|**inputs** | **Map&lt;String, Object&gt;** | The inputs the execution ran with. |  [optional] |
|**outputs** | **Map&lt;String, Object&gt;** | The outputs the execution produced. |  [optional] |
|**project** | [**ProjectReferenceModel**](ProjectReferenceModel.md) |  |  [optional] |
|**projectDeployment** | [**ProjectDeploymentReferenceModel**](ProjectDeploymentReferenceModel.md) |  |  [optional] |
|**workflow** | [**WorkflowReferenceModel**](WorkflowReferenceModel.md) |  |  [optional] |
|**taskExecutions** | [**List&lt;TaskExecutionModel&gt;**](TaskExecutionModel.md) | The executions of the workflow&#39;s tasks. |  [optional] |



