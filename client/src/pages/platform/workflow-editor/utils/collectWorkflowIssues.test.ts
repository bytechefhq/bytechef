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

    describe('references to a node that does not run before the referencing one', () => {
        const orderIssues = (issues: ReturnType<typeof collectWorkflowIssues>) =>
            issues.filter((issue) => issue.kind === 'TASK_ORDER').map((issue) => [issue.nodeName, issue.propertyPath]);

        it("reports a node in a sibling fork-join branch, keyed like the validator's task order error", () => {
            const issues = collectWorkflowIssues({
                tasks: [
                    task('forkJoin_1', {
                        branches: [
                            [task('firecrawl_7'), task('anthropic_1', {prompt: '${firecrawl_7.data.html}'})],
                            [task('firecrawl_6'), task('anthropic_2', {prompt: '${firecrawl_7.data.html}'})],
                        ],
                    }),
                ],
                triggers: [trigger('trigger_1')],
            });

            expect(issues).toEqual([
                {
                    kind: 'TASK_ORDER',
                    message:
                        '"firecrawl_7" does not run before this node, so its output is not available here (referenced as firecrawl_7.data.html)',
                    nodeName: 'anthropic_2',
                    propertyPath: 'firecrawl_7.data.html',
                    severity: 'ERROR',
                    source: 'SWEEP',
                },
            ]);
        });

        it('reads the structure from the dispatchers when the server also lists nested tasks at the top level', () => {
            const firecrawl7 = task('firecrawl_7');
            const anthropic2 = task('anthropic_2', {prompt: '${firecrawl_7.data.html}'});

            const issues = collectWorkflowIssues({
                tasks: [task('forkJoin_1', {branches: [[firecrawl7], [anthropic2]]}), firecrawl7, anthropic2],
                triggers: [trigger('trigger_1')],
            });

            expect(orderIssues(issues)).toEqual([['anthropic_2', 'firecrawl_7.data.html']]);
        });

        it('reports a node declared later and a node in the opposite condition case', () => {
            const issues = collectWorkflowIssues({
                tasks: [
                    task('logger_1', {text: '${logger_2.value}'}),
                    task('condition_1', {
                        caseFalse: [task('logger_4', {text: '${logger_3.value}'})],
                        caseTrue: [task('logger_3')],
                        expression: 'true',
                    }),
                    task('logger_2'),
                ],
                triggers: [trigger('trigger_1')],
            });

            expect(orderIssues(issues)).toEqual([
                ['logger_1', 'logger_2.value'],
                ['logger_4', 'logger_3.value'],
            ]);
        });

        it('reports a node nested inside a dispatcher that ran earlier, whose output stays in that dispatcher', () => {
            const issues = collectWorkflowIssues({
                tasks: [
                    task('condition_1', {caseTrue: [task('logger_1')], expression: 'true'}),
                    task('logger_2', {text: '${condition_1.result} ${logger_1.value}'}),
                ],
                triggers: [trigger('trigger_1')],
            });

            expect(orderIssues(issues)).toEqual([['logger_2', 'logger_1.value']]);
        });

        it('accepts earlier siblings, enclosing dispatchers and their earlier siblings, triggers and inputs', () => {
            const issues = collectWorkflowIssues({
                inputs: [{name: 'customerId'}],
                tasks: [
                    task('http_1'),
                    task('loop_1', {
                        items: '${http_1.body}',
                        iteratee: [
                            task('logger_1'),
                            task('logger_2', {
                                text: '${loop_1.item} ${logger_1.value} ${http_1.body} ${trigger_1.body} ${customerId}',
                            }),
                        ],
                    }),
                ],
                triggers: [trigger('trigger_1')],
            });

            expect(issues).toEqual([]);
        });
    });
});
