from datetime import datetime, timezone
from typing import TYPE_CHECKING, Any, Dict, List

if TYPE_CHECKING:
    from bytechef_workflow_api import ProjectDefinition, TaskContext


def fetch_orders(context: "TaskContext") -> Any:
    return context.component.httpClient.get(
        {"uri": "https://api.example.com/customers/" + context.input("customerId") + "/orders"}, "orders-api"
    )


def count_orders(context: "TaskContext") -> int:
    orders: List[Any] = context.input("fetch-orders")

    return len(orders)


def stamp(context: "TaskContext") -> str:
    return datetime.now(timezone.utc).isoformat()


def summarize(context: "TaskContext") -> Dict[str, Any]:
    context.log("info", "Summarizing orders")

    return {"count": context.input("count-orders"), "at": context.input("stamp")}


project: "ProjectDefinition" = {
    "name": "orders",
    "version": "1.0.0",
    "workflows": [
        {
            "name": "sync-orders",
            "label": "Sync orders",
            "inputs": [{"name": "customerId", "label": "Customer ID", "type": "STRING", "required": True}],
            "outputs": [{"name": "summary", "task": "summarize"}],
            "triggers": [{"name": "daily", "type": "schedule/v1/interval", "parameters": {"interval": 1, "unit": "DAY"}}],
            "tasks": [
                {
                    "name": "fetch-orders",
                    "label": "Fetch orders",
                    "connections": [{"componentName": "httpClient", "name": "orders-api"}],
                    "perform": fetch_orders,
                },
                {
                    "name": "enrich",
                    "type": "parallel",
                    "tasks": [{"name": "count-orders", "perform": count_orders}, {"name": "stamp", "perform": stamp}],
                },
                {"name": "summarize", "perform": summarize},
            ],
        }
    ],
}

project
