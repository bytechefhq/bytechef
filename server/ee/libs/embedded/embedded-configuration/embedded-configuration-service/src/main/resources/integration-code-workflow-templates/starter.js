({
    componentName: "__NAME__",
    name: "__DISPLAY_NAME__",
    componentVersion: 1,
    version: "0.0.1",
    description: "A code workflow integration.",
    workflows: [
        {
            name: "my_workflow",
            label: "My Workflow",
            // Without a trigger a workflow only runs when something calls it. Declare one to start it
            // on its own. A trigger names a component the platform provides, it is not code:
            //   triggers: [{name: "onCall", type: "workflow/v1/newWorkflowCall"}]
            //   triggers: [{name: "daily", type: "schedule/v1/interval",
            //               parameters: {interval: 1, unit: "DAY"}}]
            //
            // inputs: [{name: "orderId", label: "Order ID", type: "STRING", required: true}]
            description: "An example workflow.",
            tasks: [
                // Independent tasks can run at the same time: wrap them in a group.
                //   {name: "fan_out", type: "parallel", tasks: [ ... ]}
                //   {name: "branches", type: "forkJoin", branches: [[ ... ], [ ... ]]}
                {
                    name: "my_task",
                    label: "My Task",
                    description: "An example task.",
                    // connections: [{componentName: "httpClient", name: "my_connection"}],
                    perform: function (input, connection, context) {
                        // input holds the workflow inputs and the output of every task that already
                        // ran, each under its own name: input.orderId, input.my_earlier_task.
                        //
                        // connection maps each declared connection, by its name, to its parameters:
                        //   const credentials = connection["my_connection"];
                        //
                        // context calls component actions and writes to the execution log:
                        //
                        // const response = context.component.httpClient.get(
                        //     {uri: "https://api.example.com/items"}, "my_connection");
                        //
                        // An action that reads cluster elements, the AI Agent above all, takes
                        // them as a third argument. An element NAMES a declared connection:
                        //
                        // const answer = context.component.aiAgent.chat({messages: messages}, null, {
                        //     model: {type: "openAi/v1/model", connection: "openai_prod",
                        //             parameters: {model: "gpt-4o"}},
                        //     tools: [{type: "slack/v1/sendMessage", connection: "slack_prod",
                        //              name: "post_to_slack"}]
                        // });
                        //
                        // context.log("info", "my_task ran");

                        return "hello";
                    }
                }
            ]
        }
    ]
})
