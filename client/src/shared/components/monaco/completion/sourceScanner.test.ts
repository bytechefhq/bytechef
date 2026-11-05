import {describe, expect, it} from 'vitest';

import {
    findComponentConnectionPropertyNames,
    findEnclosingAction,
    findEnclosingTask,
    findTaskByPerformName,
} from './sourceScanner';

const CUSTOM_COMPONENT = `({
    name: "acme",
    connection: {
        properties: [
            {name: "accessToken", type: "STRING"},
            {name: "region", type: "STRING"}
        ]
    },
    actions: [
        {
            name: "first",
            properties: [{name: "query", type: "STRING"}],
            perform: function (input, connection, context) { return input.query; }
        },
        {
            name: "second",
            properties: [{name: "id", type: "STRING"}, {name: "body", type: "OBJECT"}],
            perform: function (input, connection, context) {
                input.
            }
        }
    ]
})`;

describe('findEnclosingAction', () => {
    it('returns the second action when the cursor is in its perform', () => {
        const offset = CUSTOM_COMPONENT.indexOf('input.\n') + 6;
        expect(findEnclosingAction(CUSTOM_COMPONENT, offset, 'javascript')).toEqual({
            name: 'second',
            propertyNames: ['id', 'body'],
        });
    });

    it('returns undefined above the actions array', () => {
        expect(findEnclosingAction(CUSTOM_COMPONENT, CUSTOM_COMPONENT.indexOf('"acme"'), 'javascript')).toBeUndefined();
    });

    it('tolerates an unclosed action object', () => {
        const source =
            'actions: [\n  {\n    name: "x",\n    properties: [{name: "a"}],\n    perform: function (input, connection, context) {\n      input.';
        expect(findEnclosingAction(source, source.length, 'javascript')).toEqual({name: 'x', propertyNames: ['a']});
    });

    it('reads Python dict keys', () => {
        const source =
            '"actions": [\n  {\n    "name": "x",\n    "properties": [{"name": "a"}],\n    "perform": lambda input, connection, context: input.';
        expect(findEnclosingAction(source, source.length, 'python')).toEqual({name: 'x', propertyNames: ['a']});
    });
});

describe('findComponentConnectionPropertyNames', () => {
    it('returns the component-level connection property names', () => {
        expect(findComponentConnectionPropertyNames(CUSTOM_COMPONENT, 'javascript')).toEqual(['accessToken', 'region']);
    });

    it('returns an empty list when there is no connection block', () => {
        expect(findComponentConnectionPropertyNames('({name: "x", actions: []})', 'javascript')).toEqual([]);
    });
});

const CODE_WORKFLOW = `({
    workflows: [
        {
            name: "wf",
            inputs: [{name: "orderId"}, {name: "customer"}],
            tasks: [
                {name: "fetch", perform: (context) => 1},
                {
                    name: "notify",
                    connections: [{componentName: "slack", name: "slack-prod"}],
                    perform: function (context) {
                        context.
                    }
                },
                {name: "later", perform: (context) => 3}
            ]
        }
    ]
})`;

describe('findEnclosingTask', () => {
    it('returns the task, its connections, inputs and earlier task names', () => {
        const offset = CODE_WORKFLOW.indexOf('context.\n') + 8;
        expect(findEnclosingTask(CODE_WORKFLOW, offset, 'javascript')).toEqual({
            connections: [{componentName: 'slack', componentVersion: 1, name: 'slack-prod'}],
            earlierTaskNames: ['fetch'],
            inputNames: ['orderId', 'customer'],
            name: 'notify',
        });
    });

    it('reads a connections map keyed by name', () => {
        const source =
            'tasks: [{name: "t", connections: {"gmail": {componentName: "googleMail", componentVersion: 2}}, perform: (context) => { context.';
        expect(findEnclosingTask(source, source.length, 'javascript')?.connections).toEqual([
            {componentName: 'googleMail', componentVersion: 2, name: 'gmail'},
        ]);
    });
});

describe('findTaskByPerformName', () => {
    it('finds the Python task that references a module-level function', () => {
        const source =
            'def greet(context):\n    return 1\n\nproject = {"workflows": [{"name": "w", "inputs": [{"name": "name"}], "tasks": [{"name": "greet", "perform": greet}]}]}';
        expect(findTaskByPerformName(source, 'greet', 'python')).toEqual({
            connections: [],
            earlierTaskNames: [],
            inputNames: ['name'],
            name: 'greet',
        });
    });

    it('returns undefined when no task references the name', () => {
        expect(findTaskByPerformName('def other(context):\n    pass', 'other', 'python')).toBeUndefined();
    });
});
