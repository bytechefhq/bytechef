/// <reference types="@bytechef/workflow-api" />

({
    componentName: 'httpClient',
    componentVersion: 1,
    workflows: [
        {
            inputs: [{name: 'message', required: true}],
            name: 'echo_message',
            outputs: [{name: 'echo', task: 'echo'}],
            tasks: [{name: 'echo', perform: (input, connection, context) => input.message}],
        },
    ],
}) satisfies ByteChef.IntegrationDefinition;
