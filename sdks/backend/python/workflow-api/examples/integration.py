from typing import TYPE_CHECKING, Any, Dict

if TYPE_CHECKING:
    from bytechef_workflow_api import IntegrationDefinition, TaskContext


def echo(input: Dict[str, Any], context: "TaskContext") -> Any:
    return input["message"]


integration: "IntegrationDefinition" = {
    "componentName": "httpClient",
    "componentVersion": 1,
    "workflows": [
        {
            "name": "echo_message",
            "inputs": [{"name": "message", "required": True}],
            "outputs": [{"name": "echo", "task": "echo"}],
            "tasks": [{"name": "echo", "perform": echo}],
        }
    ],
}

integration
