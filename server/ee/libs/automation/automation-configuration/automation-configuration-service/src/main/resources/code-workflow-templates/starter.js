({
    name: "__NAME__",
    version: "1",
    description: "A code workflow.",
    workflows: [
        {
            name: "my-workflow",
            label: "My Workflow",
            tasks: [
                {
                    name: "my-task",
                    label: "My Task",
                    perform: function () {
                        return "hello";
                    }
                }
            ]
        }
    ]
})
