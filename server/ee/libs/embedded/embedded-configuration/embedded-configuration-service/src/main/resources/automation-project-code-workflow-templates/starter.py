import types

# The task perform receives (input, connection, context):
#   input["orderId"]  # the workflow's inputs and every prior task's output, each under its own name
#   connection["my_connection"]  # a declared connection's parameters, keyed by its name
#   context.component.httpClient.get({"uri": "https://api.example.com/items"}, "my_connection")
# An AI agent resolves its model and tools from cluster elements, passed as a third argument; an
# element NAMES one of the task's declared connections:
#   context.component.aiAgent.chat({"messages": messages}, None, {
#       "model": {"type": "openAi/v1/model", "connection": "openai_prod",
#                 "parameters": {"model": "gpt-4o"}}
#   })
# Without a trigger a workflow only runs when something calls it. A trigger names a component the
# platform provides, it is not code; declare it on the workflow alongside "tasks":
#   "triggers": [{"name": "onCall", "type": "workflow/v1/newWorkflowCall"}]
#   "inputs": [{"name": "orderId", "label": "Order ID", "type": "STRING", "required": True}]
# Tasks can run at the same time in a group:
#   {"name": "fan_out", "type": "parallel", "tasks": [...]}
#   {"name": "branches", "type": "forkJoin", "branches": [[...], [...]]}
#   context.log("info", "my_task ran")
types.SimpleNamespace(
    name="__NAME__",
    version="1",
    description="A code workflow.",
    workflows=[
        {
            "name": "my_workflow",
            "label": "My Workflow",
            "tasks": [
                {"name": "my_task", "label": "My Task", "perform": lambda input, connection, context: "hello"}
            ]
        }
    ]
)
