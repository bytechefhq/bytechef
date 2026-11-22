/// <reference types="@bytechef/workflow-api" />

({
    name: 'orders-api',
    version: '1.0.0',
    workflows: [
        {
            label: 'List orders',
            name: 'list_orders',
            tasks: [
                {
                    connections: [{componentName: 'httpClient', name: 'orders_api'}],
                    name: 'fetch_orders',
                    perform: (input, connection, context) =>
                        context.component.httpClient.get({uri: 'https://api.example.com/orders'}, 'orders_api'),
                },
                {
                    name: 'respond',
                    parameters: {response: {success: '${fetch_orders}'}, responseType: 'SUCCESS'},
                    type: 'apiPlatform/v1/responseToAPIRequest',
                },
            ],
            triggers: [{name: 'request', parameters: {request: {}}, type: 'apiPlatform/v1/newApiRequest'}],
        },
    ],
}) satisfies ByteChef.ProjectDefinition;
