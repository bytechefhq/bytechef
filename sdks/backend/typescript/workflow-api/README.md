# @bytechef/workflow-api

TypeScript type definitions for ByteChef script code workflows. They describe the project object the ByteChef loader
reads, so the compiler catches a missing `name` or a misspelled `perform` before you deploy.

The package only carries types. ByteChef evaluates a code workflow as a plain script, so the script can't `import`
anything: reference the types with a triple-slash directive and use the global `ByteChef` namespace instead.

```ts
/// <reference types="@bytechef/workflow-api" />

({
    name: 'orders',
    version: '1.0.0',
    workflows: [
        {
            inputs: [{label: 'Customer ID', name: 'customerId', required: true, type: 'STRING'}],
            name: 'sync-orders',
            outputs: [{name: 'orders', task: 'fetch-orders'}],
            tasks: [
                {
                    connections: [{componentName: 'httpClient', name: 'orders-api'}],
                    name: 'fetch-orders',
                    perform: (context) =>
                        context.component.httpClient.get(
                            {uri: 'https://api.example.com/customers/' + context.input('customerId') + '/orders'},
                            'orders-api'
                        ),
                },
            ],
        },
    ],
}) satisfies ByteChef.ProjectDefinition;
```

`context` is typed inside `perform` without an annotation. Compile the file with `tsc` and deploy the emitted `.js`
file. `satisfies` and the directive leave nothing behind, so the
output is the plain object literal ByteChef expects. Plain JavaScript files get the same checks through `// @ts-check`
and a JSDoc `@type {ByteChef.ProjectDefinition}` annotation.

## Types

| Type | Describes |
|---|---|
| `ProjectDefinition` | An automation code project or an embedded catalog code project, identified by `name` |
| `IntegrationDefinition` | An embedded code integration, identified by `componentName` and `componentVersion` |
| `WorkflowDefinition` | A workflow: `inputs`, `outputs`, `triggers` and `tasks` |
| `InputDefinition` | A workflow input; `componentName`, `componentVersion` and `groupName` take its options from a component's declared input, such as Slack's `channel` |
| `TaskDefinition`, `ParallelTaskDefinition`, `ForkJoinTaskDefinition` | A task, and the two task groups |
| `TaskContext` | The `context` passed to `perform`: `component`, `connection`, `input`, `log`, `parameters` |
| `ConnectionRequirement`, `ClusterElement` | A task's declared connections, and cluster elements composed at a call site |

See [`examples/`](examples) for a project and an integration.

## Install

```bash
npm install --save-dev @bytechef/workflow-api
```

## Development

```bash
npm install
npm run build
npm run check:examples
```
