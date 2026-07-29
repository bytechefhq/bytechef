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
          "perform" => lambda { |*args| "hello" }
        }
      ]
    }
  ]
)
