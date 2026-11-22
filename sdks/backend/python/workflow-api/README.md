# bytechef-workflow-api

Type definitions for ByteChef script code workflows written in Python. They describe the project dict the ByteChef
loader reads, so a type checker (mypy, Pyright) catches a missing `name` or a misspelled `perform` before you deploy.

The package only carries types. The ByteChef runtime can't import it, so import it under `TYPE_CHECKING`:

```python
from typing import TYPE_CHECKING, Any, Dict

if TYPE_CHECKING:
    from bytechef_workflow_api import ProjectDefinition, TaskContext


def fetch_orders(input: Dict[str, Any], context: "TaskContext") -> Any:
    return context.component.httpClient.get(
        {"uri": "https://api.example.com/customers/" + input["customerId"] + "/orders"}, "orders_api"
    )


project: "ProjectDefinition" = {
    "name": "orders",
    "version": "1.0.0",
    "workflows": [
        {
            "name": "sync_orders",
            "inputs": [{"name": "customerId", "label": "Customer ID", "type": "STRING", "required": True}],
            "outputs": [{"name": "orders", "task": "fetch_orders"}],
            "tasks": [
                {
                    "name": "fetch_orders",
                    "connections": [{"componentName": "httpClient", "name": "orders_api"}],
                    "perform": fetch_orders,
                }
            ],
        }
    ],
}

project
```

The file's last expression, `project`, is what ByteChef loads.

`perform` takes `(input, context)`, or `(input, connection, context)` when it declares three required parameters:

- `input` holds the workflow inputs plus the output of every task that already ran, keyed by name.
- `connection` maps each connection the task declares, by its `name`, to that connection's parameters.
- `context` calls component actions (`context.component`) and writes to the execution log (`context.log`).

## Component tasks

A task with a `type` of the form `<component>/v<version>/<action>` runs that component action, the same way the task
runs in a visual workflow. Give it `parameters` instead of a `perform`; string values may hold `${...}` expressions that
read workflow inputs and earlier task outputs by name. It declares no `connections`: the deployment supplies the
component's connection. A component task can stand at the top level or inside a `parallel` or `forkJoin` group.

This is how a code workflow backs an API Platform endpoint: an `apiPlatform/v1/newApiRequest` trigger, `perform` tasks
for the logic, and a final `apiPlatform/v1/responseToAPIRequest` component task that returns the response
(see [`examples/api_endpoint.py`](examples/api_endpoint.py)):

```python
{
    "name": "respond",
    "type": "apiPlatform/v1/responseToAPIRequest",
    "parameters": {"responseType": "SUCCESS", "response": {"success": "${fetch_orders}"}},
}
```

ByteChef checks at deploy time that the component, its version and the action exist, and rejects the deploy with a
message naming the task when one doesn't.

## Types

| Type | Describes |
|---|---|
| `ProjectDefinition` | An automation code project or an embedded catalog code project, identified by `name` |
| `IntegrationDefinition` | An embedded code integration, identified by `componentName` and `componentVersion` |
| `WorkflowDefinition` | A workflow: `inputs`, `outputs`, `triggers` and `tasks` |
| `InputDefinition` | A workflow input; `componentName`, `componentVersion` and `groupName` take its options from a component's declared input, such as Slack's `channel` |
| `TaskDefinition`, `ComponentTaskDefinition`, `ParallelTaskDefinition`, `ForkJoinTaskDefinition` | A `perform` task, a component task, and the two task groups |
| `TaskContext` | The `context` passed to `perform`: `component` and `log` |
| `ConnectionRequirement`, `ClusterElement` | A task's declared connections, and cluster elements composed at a call site |

See [`examples/`](examples) for a project, an API endpoint and an integration.

## Install

```bash
pip install bytechef-workflow-api
```
