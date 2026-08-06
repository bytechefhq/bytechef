import types

# The task perform receives a context:
#   context.component.httpClient.get({"uri": "https://api.example.com/items"}, "my-connection")
# An AI agent resolves its model and tools from cluster elements, passed as a third argument; an
# element NAMES one of the task's declared connections:
#   context.component.aiAgent.chat({"messages": messages}, None, {
#       "model": {"type": "openAi/v1/model", "connection": "openai-prod",
#                 "parameters": {"model": "gpt-4o"}}
#   })
#   context.input()  # the workflow's inputs and every prior task's output, each under its own name
# Tasks can run at the same time in a group:
#   {"name": "fan-out", "type": "parallel", "tasks": [...]}
#   {"name": "branches", "type": "forkJoin", "branches": [[...], [...]]}
#   context.log("info", "my-task ran")
types.SimpleNamespace(
    name="__NAME__",
    version="1",
    description="A code workflow.",
    workflows=[
        {
            "name": "my-workflow",
            "label": "My Workflow",
            "tasks": [
                {"name": "my-task", "label": "My Task", "perform": lambda context: "hello"}
            ]
        }
    ]
)
