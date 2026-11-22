from typing import TYPE_CHECKING, Any, Dict

if TYPE_CHECKING:
    from bytechef_workflow_api import ProjectDefinition, TaskContext


def fetch_orders(input: Dict[str, Any], context: "TaskContext") -> Any:
    return context.component.httpClient.get({"uri": "https://api.example.com/orders"}, "orders_api")


project: "ProjectDefinition" = {
    "name": "orders-api",
    "version": "1.0.0",
    "workflows": [
        {
            "name": "list_orders",
            "label": "List orders",
            "triggers": [
                {"name": "request", "type": "apiPlatform/v1/newApiRequest", "parameters": {"request": {}}}
            ],
            "tasks": [
                {
                    "name": "fetch_orders",
                    "connections": [{"componentName": "httpClient", "name": "orders_api"}],
                    "perform": fetch_orders,
                },
                {
                    "name": "respond",
                    "type": "apiPlatform/v1/responseToAPIRequest",
                    "parameters": {"responseType": "SUCCESS", "response": {"success": "${fetch_orders}"}},
                },
            ],
        }
    ],
}

project
