

# WorkflowExecutionBasicModel

A workflow execution list row. Never carries execution data - fetch the execution by id for inputs, outputs and task executions.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **Long** | The id of a workflow execution. |  [readonly] |
|**status** | **WorkflowExecutionStatusModel** |  |  [optional] |
|**startDate** | **OffsetDateTime** | The instant an execution started. |  [optional] |
|**endDate** | **OffsetDateTime** | The instant an execution ended. Null while the execution is running. |  [optional] |
|**error** | [**ExecutionErrorModel**](ExecutionErrorModel.md) |  |  [optional] |
|**project** | [**ProjectReferenceModel**](ProjectReferenceModel.md) |  |  [optional] |
|**projectDeployment** | [**ProjectDeploymentReferenceModel**](ProjectDeploymentReferenceModel.md) |  |  [optional] |
|**workflow** | [**WorkflowReferenceModel**](WorkflowReferenceModel.md) |  |  [optional] |



