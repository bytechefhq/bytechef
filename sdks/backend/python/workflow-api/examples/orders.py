from datetime import datetime, timezone
from typing import TYPE_CHECKING, Any, Dict, List

if TYPE_CHECKING:
    from bytechef_workflow_api import ProjectDefinition, TaskContext


def fetch_orders(input: Dict[str, Any], context: "TaskContext") -> Any:
    return context.component.httpClient.get(
        {"uri": "https://api.example.com/customers/" + input["customerId"] + "/orders"}, "orders_api"
    )


def count_orders(input: Dict[str, Any], context: "TaskContext") -> int:
    orders: List[Any] = input["fetch_orders"]

    return len(orders)


def stamp(input: Dict[str, Any], context: "TaskContext") -> str:
    return datetime.now(timezone.utc).isoformat()


def summarize(input: Dict[str, Any], context: "TaskContext") -> Dict[str, Any]:
    context.log("info", "Summarizing orders")

    return {"count": input["count_orders"], "at": input["stamp"]}


project: "ProjectDefinition" = {
    "name": "orders",
    "version": "1.0.0",
    "workflows": [
        {
            "name": "sync_orders",
            "label": "Sync orders",
            "inputs": [{"name": "customerId", "label": "Customer ID", "type": "STRING", "required": True}],
            "outputs": [{"name": "summary", "task": "summarize"}],
            "triggers": [{"name": "daily", "type": "schedule/v1/interval", "parameters": {"interval": 1, "unit": "DAY"}}],
            "tasks": [
                {
                    "name": "fetch_orders",
                    "label": "Fetch orders",
                    "connections": [{"componentName": "httpClient", "name": "orders_api"}],
                    "perform": fetch_orders,
                },
                {
                    "name": "enrich",
                    "type": "parallel",
                    "tasks": [{"name": "count_orders", "perform": count_orders}, {"name": "stamp", "perform": stamp}],
                },
                {"name": "summarize", "perform": summarize},
            ],
        }
    ],
}

project
