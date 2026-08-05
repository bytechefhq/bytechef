({
    componentName: "__NAME__",
    componentVersion: 1,
    version: "0.0.1",
    description: "A code workflow integration.",
    workflows: [
        {
            name: "my-workflow",
            label: "My Workflow",
            description: "An example workflow.",
            tasks: [
                {
                    name: "my-task",
                    label: "My Task",
                    description: "An example task.",
                    // connections: [{componentName: "httpClient", name: "my-connection"}],
                    perform: function (context) {
                        // const response = context.component.httpClient.get(
                        //     {uri: "https://api.example.com/items"}, "my-connection");
                        //
                        // context.log("info", "my-task ran");

                        return "hello";
                    }
                }
            ]
        }
    ]
})
