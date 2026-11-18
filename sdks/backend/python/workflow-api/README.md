# bytechef-workflow-api

Type definitions for ByteChef script code workflows written in Python. They describe the project dict the ByteChef
loader reads, so a type checker (mypy, Pyright) catches a missing `name` or a misspelled `perform` before you deploy.

The package only carries types. The ByteChef runtime can't import it, so import it under `TYPE_CHECKING`:

```python
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from bytechef_workflow_api import ProjectDefinition, TaskContext


def fetch_orders(context: "TaskContext") -> Any:
    return context.component.httpClient.get(
        {"uri": "https://api.example.com/customers/" + context.input("customerId") + "/orders"}, "orders-api"
    )


project: "ProjectDefinition" = {
    "name": "orders",
    "version": "1.0.0",
    "workflows": [
        {
            "name": "sync-orders",
            "inputs": [{"name": "customerId", "label": "Customer ID", "type": "STRING", "required": True}],
            "outputs": [{"name": "orders", "task": "fetch-orders"}],
            "tasks": [
                {
                    "name": "fetch-orders",
                    "connections": [{"componentName": "httpClient", "name": "orders-api"}],
                    "perform": fetch_orders,
                }
            ],
        }
    ],
}

project
```

The file's last expression, `project`, is what ByteChef loads.

## Types

| Type | Describes |
|---|---|
| `ProjectDefinition` | An automation code project or an embedded catalog code project, identified by `name` |
| `IntegrationDefinition` | An embedded code integration, identified by `componentName` and `componentVersion` |
| `WorkflowDefinition` | A workflow: `inputs`, `outputs`, `triggers` and `tasks` |
| `TaskDefinition`, `ParallelTaskDefinition`, `ForkJoinTaskDefinition` | A task, and the two task groups |
| `TaskContext` | The `context` passed to `perform`: `component`, `connection`, `input`, `log`, `parameters` |
| `ConnectionRequirement`, `ClusterElement` | A task's declared connections, and cluster elements composed at a call site |

See [`examples/`](examples) for a project and an integration.

## Install

```bash
pip install bytechef-workflow-api
```
