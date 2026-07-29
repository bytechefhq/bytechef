Struct.new(:name, :version, :description, :workflows).new(
  "__NAME__",
  "1",
  "A code workflow.",
  [
    {
      "name" => "my-workflow",
      "label" => "My Workflow",
      "tasks" => [
        { "name" => "my-task", "label" => "My Task", "perform" => lambda { |*args| "hello" } }
      ]
    }
  ]
)
