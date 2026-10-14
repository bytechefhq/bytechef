/// <reference types="@bytechef/workflow-api" />

({
    componentName: 'httpClient',
    componentVersion: 1,
    workflows: [
        {
            inputs: [{name: 'message', required: true}],
            name: 'echo-message',
            outputs: [{name: 'echo', task: 'echo'}],
            tasks: [{name: 'echo', perform: (context) => context.input('message')}],
        },
    ],
}) satisfies ByteChef.IntegrationDefinition;
