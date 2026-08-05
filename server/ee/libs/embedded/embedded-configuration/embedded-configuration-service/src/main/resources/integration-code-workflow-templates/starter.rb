# The task perform receives a context:
#   context.component.httpClient.get({ "uri" => "https://api.example.com/items" }, "my-connection")
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
