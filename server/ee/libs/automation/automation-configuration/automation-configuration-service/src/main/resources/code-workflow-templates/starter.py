import types

# The task perform receives a context:
#   context.component.httpClient.get({"uri": "https://api.example.com/items"}, "my-connection")
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
