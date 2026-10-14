({
    componentName: "__NAME__",
    componentVersion: 1,
    version: "0.0.1",
    description: "A code workflow integration.",
    workflows: [
        {
            name: "my-workflow",
            label: "My Workflow",
            // Without a trigger a workflow only runs when something calls it. Declare one to start it
            // on its own — a trigger names a component the platform provides, it is not code:
            //   triggers: [{name: "onCall", type: "workflow/v1/newWorkflowCall"}]
            //   triggers: [{name: "daily", type: "schedule/v1/interval",
            //               parameters: {interval: 1, unit: "DAY"}}]
            //
            // inputs: [{name: "orderId", label: "Order ID", type: "STRING", required: true}]
            // outputs: [{name: "result", task: "my-task"}]
            description: "An example workflow.",
            tasks: [
                {
                    name: "my-task",
                    label: "My Task",
                    description: "An example task.",
                    // connections: [{componentName: "httpClient", name: "my-connection"}],
                    //
                    // Independent tasks can run at the same time — wrap them in a group:
                    //   {name: "fan-out", type: "parallel", tasks: [ ... ]}
                    //   {name: "branches", type: "forkJoin", branches: [[ ... ], [ ... ]]}
                    perform: function (context) {
                        // context.input() holds the workflow inputs and the output of every task that
                        // already ran, each under its own name.
                        //
                        // const response = context.component.httpClient.get(
                        //     {uri: "https://api.example.com/items"}, "my-connection");
                        //
                        // An action that reads cluster elements — the AI Agent above all — takes
                        // them as a third argument. An element NAMES a declared connection:
                        //
                        // const answer = context.component.aiAgent.chat({messages: messages}, null, {
                        //     model: {type: "openAi/v1/model", connection: "openai-prod",
                        //             parameters: {model: "gpt-4o"}},
                        //     tools: [{type: "slack/v1/sendMessage", connection: "slack-prod",
                        //              name: "post_to_slack"}]
                        // });
                        //
                        // context.log("info", "my-task ran");

                        return "hello";
                    }
                }
            ]
        }
    ]
})
