# The task perform receives a context:
#   context.component.httpClient.get({ "uri" => "https://api.example.com/items" }, "my-connection")
#   context.log("info", "my-task ran")
Struct.new(:name, :version, :description, :workflows).new(
  "__NAME__",
  "1",
  "A code workflow.",
  [
    {
      "name" => "my-workflow",
      "label" => "My Workflow",
      "tasks" => [
        { "name" => "my-task", "label" => "My Task", "perform" => lambda { |context| "hello" } }
      ]
    }
  ]
)
