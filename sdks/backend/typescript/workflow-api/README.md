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
            name: 'sync_orders',
            outputs: [{name: 'orders', task: 'fetch_orders'}],
            tasks: [
                {
                    connections: [{componentName: 'httpClient', name: 'orders_api'}],
                    name: 'fetch_orders',
                    perform: (input, connection, context) =>
                        context.component.httpClient.get(
                            {uri: 'https://api.example.com/customers/' + input.customerId + '/orders'},
                            'orders_api'
                        ),
                },
            ],
        },
    ],
}) satisfies ByteChef.ProjectDefinition;
```

`perform` takes `(input, context)`, or `(input, connection, context)` when it declares three parameters:

- `input` holds the workflow inputs plus the output of every task that already ran, keyed by name.
- `connection` maps each connection the task declares, by its `name`, to that connection's parameters.
- `context` calls component actions (`context.component`) and writes to the execution log (`context.log`).

The three-parameter form is typed inside `perform` without annotations. TypeScript can't infer the two-parameter
form's types from the union of both forms, so annotate them: `(input: ByteChef.TaskInput, context: ByteChef.TaskContext)`. Compile the file with `tsc` and deploy the emitted `.js`
file. `satisfies` and the directive leave nothing behind, so the
output is the plain object literal ByteChef expects. Plain JavaScript files get the same checks through `// @ts-check`
and a JSDoc `@type {ByteChef.ProjectDefinition}` annotation.

## Component tasks

A task with a `type` of the form `<component>/v<version>/<action>` runs that component action, the same way the task
runs in a visual workflow. Give it `parameters` instead of a `perform`; string values may hold `${...}` expressions that
read workflow inputs and earlier task outputs by name. It declares no `connections`: the deployment supplies the
component's connection. A component task can stand at the top level or inside a `parallel` or `forkJoin` group.

This is how a code workflow backs an API Platform endpoint: an `apiPlatform/v1/newApiRequest` trigger, `perform` tasks
for the logic, and a final `apiPlatform/v1/responseToAPIRequest` component task that returns the response
(see [`examples/api-endpoint.ts`](examples/api-endpoint.ts)):

```ts
{
    name: 'respond',
    parameters: {response: {success: '${fetch_orders}'}, responseType: 'SUCCESS'},
    type: 'apiPlatform/v1/responseToAPIRequest',
}
```

ByteChef checks at deploy time that the component, its version and the action exist, and rejects the deploy with a
message naming the task when one doesn't.

## Types

| Type | Describes |
|---|---|
| `ProjectDefinition` | An automation code project or an embedded catalog code project, identified by `name` |
| `IntegrationDefinition` | An embedded code integration, identified by `componentName` and `componentVersion` |
| `WorkflowDefinition` | A workflow: `inputs`, `outputs`, `triggers` and `tasks` |
| `InputDefinition` | A workflow input; `componentName`, `componentVersion` and `groupName` take its options from a component's declared input, such as Slack's `channel` |
| `TaskDefinition`, `ComponentTaskDefinition`, `ParallelTaskDefinition`, `ForkJoinTaskDefinition` | A `perform` task, a component task, and the two task groups |
| `TaskContext`, `TaskInput`, `TaskConnections` | The `context`, `input` and `connection` passed to `perform`; `context` carries `component` and `log` |
| `ConnectionRequirement`, `ClusterElement` | A task's declared connections, and cluster elements composed at a call site |

See [`examples/`](examples) for a project, an API endpoint and an integration.

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
