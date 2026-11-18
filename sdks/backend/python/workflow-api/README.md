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

## Types

| Type | Describes |
|---|---|
| `ProjectDefinition` | An automation code project or an embedded catalog code project, identified by `name` |
| `IntegrationDefinition` | An embedded code integration, identified by `componentName` and `componentVersion` |
| `WorkflowDefinition` | A workflow: `inputs`, `outputs`, `triggers` and `tasks` |
| `InputDefinition` | A workflow input; `componentName`, `componentVersion` and `groupName` take its options from a component's declared input, such as Slack's `channel` |
| `TaskDefinition`, `ParallelTaskDefinition`, `ForkJoinTaskDefinition` | A task, and the two task groups |
| `TaskContext` | The `context` passed to `perform`: `component` and `log` |
| `ConnectionRequirement`, `ClusterElement` | A task's declared connections, and cluster elements composed at a call site |

See [`examples/`](examples) for a project and an integration.

## Install

```bash
pip install bytechef-workflow-api
```
