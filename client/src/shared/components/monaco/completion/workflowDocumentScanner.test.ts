import {describe, expect, it} from 'vitest';

import {scanWorkflowDocument} from './workflowDocumentScanner';

const JSON_DOC = `{
  "label": "x",
  "triggers": [{"name": "trigger_1", "type": "schedule/v1/interval"}],
  "tasks": [
    {"name": "httpClient_1", "type": "httpClient/v1/get", "parameters": {"uri": "https://a"}},
    {
      "name": "script_1",
      "type": "script/v1/javascript",
      "connections": [{"componentName": "googleMaps", "componentVersion": 1, "name": "gm"}],
      "parameters": {
        "input": {"order": "\${httpClient_1.body}", "flag": true},
        "script": "function perform(input, connection, context) {\\n  input."
      }
    }
  ]
}`;

describe('scanWorkflowDocument json', () => {
    it('classifies the cursor inside the inline script with the task context', () => {
        const offset = JSON_DOC.lastIndexOf('input.') + 6;
        expect(scanWorkflowDocument(JSON_DOC, offset, 'json')).toMatchObject({
            earlierNodeNames: ['trigger_1', 'httpClient_1'],
            enclosingTaskConnections: [{componentName: 'googleMaps', componentVersion: 1, name: 'gm'}],
            enclosingTaskIndex: 1,
            enclosingTaskInputKeys: ['order', 'flag'],
            enclosingTaskType: 'script/v1/javascript',
            inString: true,
            path: ['tasks', '1', 'parameters', 'script'],
            stringValueBeforeCursor: 'function perform(input, connection, context) {\n  input.',
        });
    });

    it('classifies a type value', () => {
        const offset = JSON_DOC.indexOf('httpClient/v1/get') + 'httpClient/v1/get'.length;
        expect(scanWorkflowDocument(JSON_DOC, offset, 'json')).toMatchObject({
            inString: true,
            path: ['tasks', '0', 'type'],
            stringValueBeforeCursor: 'httpClient/v1/get',
        });
    });

    it('classifies an expression string', () => {
        const offset = JSON_DOC.indexOf('${httpClient_1.body}') + '${httpClient_1.'.length;
        expect(scanWorkflowDocument(JSON_DOC, offset, 'json')).toMatchObject({
            earlierNodeNames: ['trigger_1', 'httpClient_1'],
            inString: true,
            path: ['tasks', '1', 'parameters', 'input', 'order'],
            stringValueBeforeCursor: '${httpClient_1.',
        });
    });

    it('tolerates an unterminated string and a missing close brace', () => {
        const source =
            '{"tasks": [{"name": "a", "type": "script/v1/javascript", "parameters": {"script": "function perform(input, connection, context) { input.';
        expect(scanWorkflowDocument(source, source.length, 'json')).toMatchObject({
            enclosingTaskIndex: 0,
            inString: true,
            path: ['tasks', '0', 'parameters', 'script'],
        });
    });

    it('reports not in a string between tokens', () => {
        const offset = JSON_DOC.indexOf('"tasks"') - 1;
        expect(scanWorkflowDocument(JSON_DOC, offset, 'json')).toMatchObject({inString: false});
    });
});

const YAML_DOC = `label: x
triggers:
  - name: trigger_1
    type: schedule/v1/interval
tasks:
  - name: httpClient_1
    type: httpClient/v1/get
  - name: script_1
    type: script/v1/python
    parameters:
      input:
        order: \${httpClient_1.body}
      script: |
        def perform(input, connection, context):
            return input.
`;

describe('scanWorkflowDocument yaml', () => {
    it('classifies the cursor inside a block scalar script', () => {
        const offset = YAML_DOC.lastIndexOf('input.') + 6;
        expect(scanWorkflowDocument(YAML_DOC, offset, 'yaml')).toMatchObject({
            earlierNodeNames: ['trigger_1', 'httpClient_1'],
            enclosingTaskIndex: 1,
            enclosingTaskInputKeys: ['order'],
            enclosingTaskType: 'script/v1/python',
            inString: true,
            path: ['tasks', '1', 'parameters', 'script'],
            stringValueBeforeCursor: 'def perform(input, connection, context):\n    return input.',
        });
    });

    it('classifies a plain scalar type value', () => {
        const offset = YAML_DOC.indexOf('httpClient/v1/get') + 'httpClient/v1/get'.length;
        expect(scanWorkflowDocument(YAML_DOC, offset, 'yaml')).toMatchObject({
            inString: true,
            path: ['tasks', '0', 'type'],
            stringValueBeforeCursor: 'httpClient/v1/get',
        });
    });

    it('classifies a plain scalar expression', () => {
        const offset = YAML_DOC.indexOf('${httpClient_1.body}') + '${httpClient_1.'.length;
        expect(scanWorkflowDocument(YAML_DOC, offset, 'yaml')).toMatchObject({
            inString: true,
            path: ['tasks', '1', 'parameters', 'input', 'order'],
            stringValueBeforeCursor: '${httpClient_1.',
        });
    });
});

describe('scanWorkflowDocument edge cases', () => {
    it('returns undefined inside comments and for an unknown format', () => {
        const json = '{\n  // note\n  "a": 1\n}';
        const yaml = 'a: 1 # note\nb: 2\n';

        expect(scanWorkflowDocument(json, json.indexOf('note'), 'json')).toBeUndefined();
        expect(scanWorkflowDocument(yaml, yaml.indexOf('note'), 'yaml')).toBeUndefined();
        expect(scanWorkflowDocument(yaml, 1, 'toml' as 'json')).toBeUndefined();
    });

    it('accepts a trailing comma and reads connections declared as a map', () => {
        const source =
            '{"tasks": [{"name": "a", "connections": {"gm": {"componentName": "googleMaps"}}, "parameters": {"script": "x",}},]}';
        const offset = source.indexOf('"x"') + 2;

        expect(scanWorkflowDocument(source, offset, 'json')).toMatchObject({
            enclosingTaskConnections: [{componentName: 'googleMaps', componentVersion: 1, name: 'gm'}],
            enclosingTaskIndex: 0,
            inString: true,
            path: ['tasks', '0', 'parameters', 'script'],
        });
    });

    it('classifies double-quoted yaml strings and the not-in-string case', () => {
        const source = 'tasks:\n  - name: a\n    type: "script/v1/ja"\n';
        const offset = source.indexOf('ja') + 2;

        expect(scanWorkflowDocument(source, offset, 'yaml')).toMatchObject({
            inString: true,
            path: ['tasks', '0', 'type'],
            stringValueBeforeCursor: 'script/v1/ja',
        });
        expect(scanWorkflowDocument(source, source.indexOf('tasks:'), 'yaml')).toMatchObject({inString: false});
    });

    it('classifies a freshly opened blank line inside a block scalar', () => {
        const source = 'tasks:\n  - name: a\n    parameters:\n      script: |\n        x = 1\n        ';

        expect(scanWorkflowDocument(source, source.length, 'yaml')).toMatchObject({
            enclosingTaskIndex: 0,
            inString: true,
            stringValueBeforeCursor: 'x = 1\n',
        });
    });
});

describe('scanWorkflowDocument empty values', () => {
    it('classifies an empty yaml value at the end of the document', () => {
        const source = 'tasks:\n  - name: t\n    type: ';

        expect(scanWorkflowDocument(source, source.length, 'yaml')).toMatchObject({
            inString: true,
            path: ['tasks', '0', 'type'],
            stringValueBeforeCursor: '',
        });
    });

    it('classifies an empty yaml value when more keys follow', () => {
        const source = 'tasks:\n  - name: t\n    type: \n    label: x\n';
        const offset = source.indexOf('type: ') + 'type: '.length;

        expect(scanWorkflowDocument(source, offset, 'yaml')).toMatchObject({
            enclosingTaskIndex: 0,
            inString: true,
            path: ['tasks', '0', 'type'],
            stringValueBeforeCursor: '',
        });
    });

    it('classifies an empty json string between the quotes', () => {
        const source = '{"tasks": [{"name": "t", "type": ""}]}';
        const offset = source.indexOf('""') + 1;

        expect(scanWorkflowDocument(source, offset, 'json')).toMatchObject({
            inString: true,
            path: ['tasks', '0', 'type'],
            stringValueBeforeCursor: '',
        });
    });
});
