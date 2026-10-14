from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from bytechef_workflow_api import IntegrationDefinition, TaskContext


def echo(context: "TaskContext") -> Any:
    return context.input("message")


integration: "IntegrationDefinition" = {
    "componentName": "httpClient",
    "componentVersion": 1,
    "workflows": [
        {
            "name": "echo-message",
            "inputs": [{"name": "message", "required": True}],
            "outputs": [{"name": "echo", "task": "echo"}],
            "tasks": [{"name": "echo", "perform": echo}],
        }
    ],
}

integration
