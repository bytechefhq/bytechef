# The task perform receives a context:
#   context.component.httpClient.get({ "uri" => "https://api.example.com/items" }, "my-connection")
# An AI agent resolves its model and tools from cluster elements, passed as a third argument; an
# element NAMES one of the task's declared connections:
#   context.component.aiAgent.chat({ "messages" => messages }, nil, {
#     "model" => { "type" => "openAi/v1/model", "connection" => "openai-prod",
#                  "parameters" => { "model" => "gpt-4o" } }
#   })
#   context.input  # the workflow's inputs and every prior task's output, each under its own name
# Tasks can run at the same time in a group:
#   { "name" => "fan-out", "type" => "parallel", "tasks" => [...] }
#   { "name" => "branches", "type" => "forkJoin", "branches" => [[...], [...]] }
#   context.log("info", "my-task ran")
Struct.new(:componentName, :componentVersion, :version, :description, :workflows).new(
  "__NAME__",
  1,
  "0.0.1",
  "A code workflow integration.",
  [
    {
      "name" => "my-workflow",
      "label" => "My Workflow",
      "description" => "An example workflow.",
      "tasks" => [
        {
          "name" => "my-task",
          "label" => "My Task",
          "description" => "An example task.",
          "perform" => lambda { |context| "hello" }
        }
      ]
    }
  ]
)
