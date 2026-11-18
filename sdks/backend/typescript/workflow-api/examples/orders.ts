/// <reference types="@bytechef/workflow-api" />

({
    name: 'orders',
    version: '1.0.0',
    workflows: [
        {
            inputs: [{label: 'Customer ID', name: 'customerId', required: true, type: 'STRING'}],
            label: 'Sync orders',
            name: 'sync_orders',
            outputs: [{name: 'summary', task: 'summarize'}],
            tasks: [
                {
                    connections: [{componentName: 'httpClient', name: 'orders_api'}],
                    label: 'Fetch orders',
                    name: 'fetch_orders',
                    perform: (input, connection, context) =>
                        context.component.httpClient.get(
                            {uri: 'https://api.example.com/customers/' + input.customerId + '/orders'},
                            'orders_api',
                        ),
                },
                {
                    name: 'enrich',
                    tasks: [
                        {
                            name: 'count_orders',
                            perform: (input: ByteChef.TaskInput, context: ByteChef.TaskContext) =>
                                (input['fetch_orders'] as unknown[]).length,
                        },
                        {name: 'stamp', perform: (input, connection, context) => new Date().toISOString()},
                    ],
                    type: 'parallel',
                },
                {
                    name: 'summarize',
                    perform: (input, connection, context) => {
                        context.log('info', 'Summarizing orders');

                        return {count: input['count_orders'], at: input.stamp};
                    },
                },
            ],
            triggers: [{name: 'daily', parameters: {interval: 1, unit: 'DAY'}, type: 'schedule/v1/interval'}],
        },
    ],
}) satisfies ByteChef.ProjectDefinition;
