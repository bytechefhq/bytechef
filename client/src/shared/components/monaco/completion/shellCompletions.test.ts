import {describe, expect, it} from 'vitest';

import {getShellCompletions} from './shellCompletions';

import type {ShellCompletionContextI} from './completionTypes';

const context: ShellCompletionContextI = {
    connections: [],
    env: ['API_TOKEN', 'not-a-name'],
    input: {channel: '#general', "it's": 1, 'max items': 5},
    kind: 'shell',
};

const at = (source: string) => getShellCompletions({context, cursorOffset: source.length, source});

const labels = (source: string) => at(source).map((item) => item.label);

describe('getShellCompletions', () => {
    describe('after a $', () => {
        it('offers the task runner variables, the environment and the variables the script declares', () => {
            const source = 'count=0\nexport TARGET=prod\nfor file in *.txt; do\n  read -r first second\ndone\necho $';

            expect(labels(source)).toEqual([
                'BYTECHEF_INPUT_FILE',
                'BYTECHEF_OUTPUT_DIR',
                'BYTECHEF_WORKING_DIR',
                'BYTECHEF_CONNECTIONS_FILE',
                'INPUT_channel',
                'API_TOKEN',
                'count',
                'TARGET',
                'file',
                'first',
                'second',
            ]);
        });

        it('replaces only the name typed after the $', () => {
            const item = at('cat "$BYTE').find((candidate) => candidate.label === 'BYTECHEF_INPUT_FILE');

            expect(item).toMatchObject({insertText: 'BYTECHEF_INPUT_FILE', kind: 'variable', replaceLength: 4});
        });

        it('completes a braced reference', () => {
            const item = at('echo "${BYTECHEF_O').find((candidate) => candidate.label === 'BYTECHEF_OUTPUT_DIR');

            expect(item).toMatchObject({insertText: 'BYTECHEF_OUTPUT_DIR', replaceLength: 10});
        });

        it('offers each input value as its INPUT_ variable', () => {
            const item = at('echo $INPUT_').find((candidate) => candidate.label === 'INPUT_channel');

            expect(item).toMatchObject({detail: 'Input', insertText: 'INPUT_channel', replaceLength: 'INPUT_'.length});
        });

        it('documents the task runner variables', () => {
            const item = at('echo $').find((candidate) => candidate.label === 'BYTECHEF_OUTPUT_DIR');

            expect(item?.detail).toBe('Task runner');
            expect(item?.documentation).toContain('output.json');
        });

        it('offers nothing for an escaped $', () => {
            expect(at('echo \\$')).toEqual([]);
        });
    });

    describe('where no expansion happens', () => {
        it('offers nothing in a comment', () => {
            expect(at('# read $')).toEqual([]);
            expect(at('echo hi # then input.')).toEqual([]);
        });

        it('offers nothing inside single quotes', () => {
            expect(at("echo '$")).toEqual([]);
        });

        it('still completes after a quoted # that does not start a comment', () => {
            expect(labels('echo "#tag" $')).toContain('BYTECHEF_INPUT_FILE');
        });
    });

    describe('at a word', () => {
        it('expands an input value from its INPUT_ variable, quoted, and offers to write output.json', () => {
            const items = at('channel=input.');

            expect(items.map((item) => item.label)).toEqual(['input.channel', 'output.json']);
            expect(items[0]).toMatchObject({insertText: '"$INPUT_channel"', replaceLength: 'input.'.length});
        });

        it('leaves out an input key that is not a shell variable name', () => {
            expect(labels('x=')).not.toContain('input.max items');
            expect(labels('x=')).not.toContain("input.it's");
        });

        it('drops the quotes and the output.json command inside double quotes', () => {
            const items = at('echo "Posting to input.ch');

            expect(items.map((item) => item.label)).toEqual(['input.channel']);
            expect(items[0]).toMatchObject({insertText: '$INPUT_channel', replaceLength: 'input.ch'.length});
        });

        it('offers to read each connection from the connections file', () => {
            const items = getShellCompletions({
                context: {...context, connections: [{componentName: 'slack', componentVersion: 1, name: 'slack'}]},
                cursorOffset: 'token=conn'.length,
                source: 'token=conn',
            });

            const connectionItem = items.find((item) => item.label === 'connection.slack');

            expect(connectionItem).toMatchObject({
                detail: 'Connection (slack)',
                insertAsSnippet: true,
                insertText: `jq -r '.slack.\${1:parameter}' "\\$BYTECHEF_CONNECTIONS_FILE"`,
                replaceLength: 'conn'.length,
            });
        });

        it('wraps the connection read in a command substitution inside double quotes', () => {
            const source = 'curl -H "Authorization: Bearer conn';

            const items = getShellCompletions({
                context: {...context, connections: [{componentName: 'slack', componentVersion: 1, name: 'slack'}]},
                cursorOffset: source.length,
                source,
            });

            const connectionItem = items.find((item) => item.label === 'connection.slack');

            expect(connectionItem?.insertText).toBe(
                `\\$(jq -r '.slack.\${1:parameter}' "\\$BYTECHEF_CONNECTIONS_FILE")`
            );
        });

        it('quotes a connection name that is not a jq identifier and escapes it for the snippet', () => {
            const items = getShellCompletions({
                context: {...context, connections: [{componentName: 'http', componentVersion: 1, name: 'my-$api'}]},
                cursorOffset: 0,
                source: '',
            });

            const connectionItem = items.find((item) => item.label === 'connection.my-$api');

            expect(connectionItem?.insertText).toBe(
                `jq -r '.["my-\\$api"].\${1:parameter}' "\\$BYTECHEF_CONNECTIONS_FILE"`
            );
        });

        it('writes output.json under the output directory without extra tools', () => {
            const item = at('').find((candidate) => candidate.label === 'output.json');

            expect(item?.insertText).toBe(`printf '{"value": "%s"}\\n' "$value" > "$BYTECHEF_OUTPUT_DIR/output.json"`);
        });
    });
});
