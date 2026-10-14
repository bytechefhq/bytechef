/// <reference types="@bytechef/workflow-api" />

({
    name: 'orders',
    version: '1.0.0',
    workflows: [
        {
            inputs: [{label: 'Customer ID', name: 'customerId', required: true, type: 'STRING'}],
            label: 'Sync orders',
            name: 'sync-orders',
            outputs: [{name: 'summary', task: 'summarize'}],
            tasks: [
                {
                    connections: [{componentName: 'httpClient', name: 'orders-api'}],
                    label: 'Fetch orders',
                    name: 'fetch-orders',
                    perform: (context) =>
                        context.component.httpClient.get(
                            {uri: 'https://api.example.com/customers/' + context.input('customerId') + '/orders'},
                            'orders-api',
                        ),
                },
                {
                    name: 'enrich',
                    tasks: [
                        {
                            name: 'count-orders',
                            perform: (context) => (context.input('fetch-orders') as unknown[]).length,
                        },
                        {name: 'stamp', perform: () => new Date().toISOString()},
                    ],
                    type: 'parallel',
                },
                {
                    name: 'summarize',
                    perform: (context) => {
                        context.log('info', 'Summarizing orders');

                        return {count: context.input('count-orders'), at: context.input('stamp')};
                    },
                },
            ],
            triggers: [{name: 'daily', parameters: {interval: 1, unit: 'DAY'}, type: 'schedule/v1/interval'}],
        },
    ],
}) satisfies ByteChef.ProjectDefinition;
