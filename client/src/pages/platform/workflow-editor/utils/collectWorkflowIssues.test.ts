import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import collectWorkflowIssues from './collectWorkflowIssues';

const task = (name: string, parameters: Record<string, unknown> = {}): WorkflowTask =>
    ({name, parameters, type: 'example/v1/action'}) as WorkflowTask;
const trigger = (name: string): WorkflowTrigger => ({name, type: 'manual/v1/manual'}) as WorkflowTrigger;

describe('collectWorkflowIssues', () => {
    it('returns nothing for a workflow whose references all resolve', () => {
        const issues = collectWorkflowIssues({
            tasks: [task('python_1'), task('condition_1', {expression: '${python_1.diff} > 0'})],
            triggers: [trigger('trigger_1')],
        });

        expect(issues).toEqual([]);
    });

    it('reports a reference whose root node is missing, keyed on the expression', () => {
        const issues = collectWorkflowIssues({
            tasks: [task('condition_1', {expression: '${python_1.diff} > 0'})],
            triggers: [trigger('trigger_1')],
        });

        expect(issues).toEqual([
            {
                kind: 'BROKEN_REFERENCE',
                message: '"python_1" is missing from the workflow (referenced as python_1.diff)',
                nodeName: 'condition_1',
                propertyPath: 'python_1.diff',
                severity: 'ERROR',
                source: 'SWEEP',
            },
        ]);
    });

    it('accepts workflow inputs and trigger names as roots and ignores function expressions', () => {
        const issues = collectWorkflowIssues({
            inputs: [{name: 'customerId'}],
            tasks: [task('logger_1', {text: '${customerId} ${trigger_1.body} ${=1 + 1}'})],
            triggers: [trigger('trigger_1')],
        });

        expect(issues).toEqual([]);
    });

    it('walks nested tasks and their parameters without treating nested task arrays as values', () => {
        const issues = collectWorkflowIssues({
            tasks: [
                task('condition_1', {
                    caseTrue: [task('logger_1', {text: '${ghost_1.value}'})],
                    expression: 'true',
                }),
            ],
            triggers: [trigger('trigger_1')],
        });

        expect(issues.map((issue) => [issue.nodeName, issue.propertyPath])).toEqual([['logger_1', 'ghost_1.value']]);
    });

    it('ignores function call expressions like now() while still reporting broken references', () => {
        const issues = collectWorkflowIssues({
            tasks: [task('logger_1', {text: '${now()} ${ghost_1.value}'})],
            triggers: [],
        });

        expect(issues).toEqual([
            {
                kind: 'BROKEN_REFERENCE',
                message: '"ghost_1" is missing from the workflow (referenced as ghost_1.value)',
                nodeName: 'logger_1',
                propertyPath: 'ghost_1.value',
                severity: 'ERROR',
                source: 'SWEEP',
            },
        ]);
    });

    it('reports each missing reference once per node even when repeated', () => {
        const issues = collectWorkflowIssues({
            tasks: [task('logger_1', {a: '${ghost_1.x}', b: '${ghost_1.x}'})],
            triggers: [],
        });

        expect(issues).toHaveLength(1);
    });

    it('reports duplicate node names attributed to the duplicated name', () => {
        const issues = collectWorkflowIssues({tasks: [task('logger_1'), task('logger_1')], triggers: []});

        expect(issues).toEqual([
            {
                kind: 'DUPLICATE_NODE_NAME',
                message: 'Node names must be unique. Duplicate node name: logger_1',
                nodeName: 'logger_1',
                severity: 'ERROR',
                source: 'SWEEP',
            },
        ]);
    });

    it('pins the duplicate node name message to the literal the server validator produces', () => {
        const issues = collectWorkflowIssues({tasks: [task('logger_1'), task('logger_1')], triggers: []});

        expect(issues[0].message).toBe('Node names must be unique. Duplicate node name: logger_1');
    });

    it('does not report a duplicate for a task the server lists both at the top level and inside its dispatcher', () => {
        const issues = collectWorkflowIssues({
            tasks: [
                task('condition_1', {
                    caseTrue: [task('logger_1')],
                    expression: 'true',
                }),
                task('logger_1'),
            ],
            triggers: [],
        });

        expect(issues.filter((issue) => issue.kind === 'DUPLICATE_NODE_NAME')).toEqual([]);
    });

    it('reports a duplicate node name when the server lists the same name twice at the top level', () => {
        const issues = collectWorkflowIssues({
            tasks: [
                task('condition_1', {
                    caseTrue: [task('logger_1')],
                    expression: 'true',
                }),
                task('logger_1'),
                task('logger_1'),
            ],
            triggers: [],
        });

        expect(issues.filter((issue) => issue.kind === 'DUPLICATE_NODE_NAME')).toEqual([
            {
                kind: 'DUPLICATE_NODE_NAME',
                message: 'Node names must be unique. Duplicate node name: logger_1',
                nodeName: 'logger_1',
                severity: 'ERROR',
                source: 'SWEEP',
            },
        ]);
    });
});
