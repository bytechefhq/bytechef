import types

# The task perform receives a context:
#   context.component.httpClient.get({"uri": "https://api.example.com/items"}, "my-connection")
#   context.log("info", "my-task ran")
types.SimpleNamespace(
    componentName="__NAME__",
    componentVersion=1,
    version="0.0.1",
    description="A code workflow integration.",
    workflows=[
        {
            "name": "my-workflow",
            "label": "My Workflow",
            "description": "An example workflow.",
            "tasks": [
                {
                    "name": "my-task",
                    "label": "My Task",
                    "description": "An example task.",
                    "perform": lambda context: "hello",
                }
            ],
        }
    ],
)
