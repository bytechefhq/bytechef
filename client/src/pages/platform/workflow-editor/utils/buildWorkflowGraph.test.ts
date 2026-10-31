import {FINAL_PLACEHOLDER_NODE_ID, TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {ComponentDefinitionBasic, WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import buildWorkflowGraph from './buildWorkflowGraph';

const TRIGGER: WorkflowTrigger = {name: 'trigger_1', type: 'manual/v1/manual'};

function buildTask(name: string, type = 'httpClient/v1/get', parameters: Record<string, unknown> = {}): WorkflowTask {
    return {name, parameters, type};
}

function build(tasks: WorkflowTask[] | undefined, triggers: WorkflowTrigger[] | undefined = [TRIGGER]) {
    return buildWorkflowGraph({
        canvasWidth: 800,
        componentDefinitions: [],
        taskDispatcherDefinitions: [],
        tasks,
        triggers,
    });
}

describe('buildWorkflowGraph', () => {
    it('builds trigger, task and placeholder nodes in order for a linear workflow', () => {
        const {nodes} = build([buildTask('t1'), buildTask('t2')]);

        expect(nodes.map((node) => node.id)).toEqual([
            'trigger_1',
            TRIGGER_PLACEHOLDER_NODE_ID,
            't1',
            't2',
            FINAL_PLACEHOLDER_NODE_ID,
        ]);
    });

    it('creates an edge between consecutive tasks and a fan-in edge from the trigger', () => {
        const {edges} = build([buildTask('t1'), buildTask('t2')]);

        expect(edges.map((edge) => edge.id)).toEqual([`trigger_1=>t1`, `t1=>t2`, `t2=>${FINAL_PLACEHOLDER_NODE_ID}`]);
    });

    it('builds condition child nodes with ghost nodes', () => {
        const {nodes} = build([buildTask('condition_1', 'condition/v1', {caseFalse: [], caseTrue: []})]);

        const nodeIds = nodes.map((node) => node.id);

        expect(nodeIds).toContain('condition_1');
        expect(nodeIds).toContain('condition_1-condition-bottom-ghost');
        expect(nodeIds[nodeIds.length - 1]).toBe(FINAL_PLACEHOLDER_NODE_ID);
    });

    it('returns only placeholders when there are no tasks', () => {
        const {edges, nodes} = build([]);

        expect(nodes.map((node) => node.id)).toEqual([
            'trigger_1',
            TRIGGER_PLACEHOLDER_NODE_ID,
            FINAL_PLACEHOLDER_NODE_ID,
        ]);
        expect(edges.map((edge) => edge.id)).toEqual([`trigger_1=>${FINAL_PLACEHOLDER_NODE_ID}`]);
    });

    it('returns fresh arrays on every call', () => {
        const tasks = [buildTask('t1')];

        const first = build(tasks);
        const second = build(tasks);

        expect(first.nodes).not.toBe(second.nodes);
        expect(first.edges).not.toBe(second.edges);
    });

    describe('task dispatchers', () => {
        function edgeIds(tasks: WorkflowTask[]) {
            return build(tasks).edges.map((edge) => edge.id);
        }

        function nodeIds(tasks: WorkflowTask[]) {
            return build(tasks).nodes.map((node) => node.id);
        }

        it('wires condition children between the top and bottom ghost nodes', () => {
            const tasks = [
                buildTask('condition_1', 'condition/v1', {
                    caseFalse: [buildTask('f1')],
                    caseTrue: [buildTask('a1'), buildTask('a2')],
                }),
                buildTask('a1'),
                buildTask('a2'),
                buildTask('f1'),
                buildTask('after'),
            ];

            const {edges, nodes} = build(tasks);

            expect(nodes.map((node) => node.id)).not.toContain('condition_1-condition-right-placeholder-0');
            expect(edges.map((edge) => edge.id)).toEqual(
                expect.arrayContaining([
                    'condition_1-condition-top-ghost=>a1',
                    'a1=>a2',
                    'a2=>condition_1-condition-bottom-ghost',
                    'condition_1-condition-top-ghost=>f1',
                    'f1=>condition_1-condition-bottom-ghost',
                    'condition_1-condition-bottom-ghost=>after',
                ])
            );
            expect(edges.find((edge) => edge.id === 'a2=>condition_1-condition-bottom-ghost')?.targetHandle).toBe(
                'condition_1-condition-bottom-ghost-left'
            );
            expect(edges.find((edge) => edge.id === 'f1=>condition_1-condition-bottom-ghost')?.targetHandle).toBe(
                'condition_1-condition-bottom-ghost-right'
            );
            expect(edges.map((edge) => edge.id)).not.toContain('a2=>f1');

            const childData = nodes.find((node) => node.id === 'a1')?.data as Record<string, unknown>;

            expect(childData.conditionData).toMatchObject({conditionCase: 'caseTrue', conditionId: 'condition_1'});
        });

        it('creates a placeholder for the empty condition branch', () => {
            const tasks = [
                buildTask('condition_1', 'condition/v1', {caseFalse: [], caseTrue: [buildTask('a1')]}),
                buildTask('a1'),
                buildTask('after'),
            ];

            const {edges, nodes} = build(tasks);

            const placeholderNode = nodes.find((node) => node.id === 'condition_1-condition-right-placeholder-0');

            expect(placeholderNode?.type).toBe('placeholder');
            expect(edges.map((edge) => edge.id)).toEqual(
                expect.arrayContaining([
                    'condition_1-condition-top-ghost=>condition_1-condition-right-placeholder-0',
                    'condition_1-condition-right-placeholder-0=>condition_1-condition-bottom-ghost',
                ])
            );
        });

        it('wires loop children and creates a left ghost', () => {
            const tasks = [
                buildTask('loop_1', 'loop/v1', {iteratee: [buildTask('l1')]}),
                buildTask('l1'),
                buildTask('after'),
            ];

            const {edges, nodes} = build(tasks);

            expect(nodes.map((node) => node.id)).toEqual(
                expect.arrayContaining([
                    'loop_1-loop-top-ghost',
                    'loop_1-taskDispatcher-left-ghost',
                    'loop_1-loop-bottom-ghost',
                ])
            );
            expect(nodes.map((node) => node.id)).not.toContain('loop_1-loop-placeholder-0');
            expect(edges.map((edge) => edge.id)).toEqual(
                expect.arrayContaining([
                    'loop_1-loop-top-ghost=>l1',
                    'l1=>loop_1-loop-bottom-ghost',
                    'loop_1-loop-bottom-ghost=>after',
                ])
            );
        });

        it('creates a placeholder for a loop without iteratee tasks', () => {
            const ids = nodeIds([buildTask('loop_1', 'loop/v1', {iteratee: []}), buildTask('after')]);

            expect(ids).toContain('loop_1-loop-placeholder-0');
        });

        it('wires map children and creates a placeholder when the iteratee is empty', () => {
            const populatedEdges = edgeIds([
                buildTask('map_1', 'map/v1', {iteratee: [buildTask('m1')]}),
                buildTask('m1'),
                buildTask('after'),
            ]);

            expect(populatedEdges).toEqual(
                expect.arrayContaining([
                    'map_1-map-top-ghost=>m1',
                    'm1=>map_1-map-bottom-ghost',
                    'map_1-map-bottom-ghost=>after',
                ])
            );

            expect(nodeIds([buildTask('map_1', 'map/v1', {iteratee: []}), buildTask('after')])).toContain(
                'map_1-map-placeholder-0'
            );
        });

        it('wires on-error main and error branch children', () => {
            const tasks = [
                buildTask('onError_1', 'on-error/v1', {
                    'main-branch': [buildTask('m1')],
                    'on-error-branch': [buildTask('e1')],
                }),
                buildTask('m1'),
                buildTask('e1'),
                buildTask('after'),
            ];

            const {edges, nodes} = build(tasks);

            expect(nodes.map((node) => node.id)).not.toContain('onError_1-onError-left-placeholder-0');
            expect(edges.map((edge) => edge.id)).toEqual(
                expect.arrayContaining([
                    'onError_1-onError-top-ghost=>m1',
                    'm1=>onError_1-onError-bottom-ghost',
                    'onError_1-onError-top-ghost=>e1',
                    'e1=>onError_1-onError-bottom-ghost',
                    'onError_1-onError-bottom-ghost=>after',
                ])
            );
            expect(edges.map((edge) => edge.id)).not.toContain('m1=>e1');

            const errorChildData = nodes.find((node) => node.id === 'e1')?.data as Record<string, unknown>;

            expect(errorChildData.onErrorData).toMatchObject({onErrorId: 'onError_1'});
        });

        it('creates placeholders for both empty on-error branches', () => {
            const {edges, nodes} = build([buildTask('onError_1', 'on-error/v1', {}), buildTask('after')]);

            expect(nodes.map((node) => node.id)).toEqual(
                expect.arrayContaining([
                    'onError_1-onError-left-placeholder-0',
                    'onError_1-onError-right-placeholder-0',
                ])
            );
            expect(edges.map((edge) => edge.id)).toEqual(
                expect.arrayContaining([
                    'onError_1-onError-top-ghost=>onError_1-onError-left-placeholder-0',
                    'onError_1-onError-right-placeholder-0=>onError_1-onError-bottom-ghost',
                ])
            );
        });

        it('wires branch case and default children and a placeholder for an empty case', () => {
            const tasks = [
                buildTask('branch_1', 'branch/v1', {
                    cases: [
                        {key: 'c1', tasks: [buildTask('b1')]},
                        {key: 'c2', tasks: []},
                    ],
                    default: [buildTask('d1')],
                }),
                buildTask('b1'),
                buildTask('d1'),
                buildTask('after'),
            ];

            const {edges, nodes} = build(tasks);

            expect(nodes.map((node) => node.id)).toContain('branch_1-branch-c2-placeholder-0');
            expect(edges.map((edge) => edge.id)).toEqual(
                expect.arrayContaining([
                    'branch_1-branch-top-ghost=>b1',
                    'b1=>branch_1-branch-bottom-ghost',
                    'branch_1-branch-top-ghost=>d1',
                    'branch_1-branch-top-ghost=>branch_1-branch-c2-placeholder-0',
                    'branch_1-branch-bottom-ghost=>after',
                ])
            );
            expect(
                edges.find((edge) => edge.id === 'branch_1-branch-top-ghost=>branch_1-branch-c2-placeholder-0')?.type
            ).toBe('labeledBranchCase');
        });

        it('creates a default placeholder for a branch without default tasks', () => {
            const ids = nodeIds([buildTask('branch_1', 'branch/v1', {cases: [], default: []}), buildTask('after')]);

            expect(ids.some((id) => id.startsWith('branch_1-branch-') && id.includes('placeholder'))).toBe(true);
        });

        it('wires parallel lanes and adds a ghost placeholder for a single lane', () => {
            const multipleLaneEdges = edgeIds([
                buildTask('parallel_1', 'parallel/v1', {tasks: [buildTask('p1'), buildTask('p2')]}),
                buildTask('p1'),
                buildTask('p2'),
                buildTask('after'),
            ]);

            expect(multipleLaneEdges).toEqual(
                expect.arrayContaining([
                    'parallel_1-parallel-top-ghost=>p1',
                    'p1=>parallel_1-parallel-bottom-ghost',
                    'parallel_1-parallel-top-ghost=>p2',
                    'p2=>parallel_1-parallel-bottom-ghost',
                    'parallel_1-parallel-bottom-ghost=>after',
                ])
            );

            const singleLaneNodeIds = nodeIds([
                buildTask('parallel_1', 'parallel/v1', {tasks: [buildTask('p1')]}),
                buildTask('p1'),
            ]);

            expect(singleLaneNodeIds.some((id) => id.startsWith('parallel_1-parallel-placeholder'))).toBe(true);
        });

        it('wires each iteratee and creates a placeholder when there is none', () => {
            const populatedEdges = edgeIds([
                buildTask('each_1', 'each/v1', {iteratee: buildTask('e1')}),
                buildTask('e1'),
                buildTask('after'),
            ]);

            expect(populatedEdges).toEqual(
                expect.arrayContaining([
                    'each_1-each-top-ghost=>e1',
                    'e1=>each_1-each-bottom-ghost',
                    'each_1-each-bottom-ghost=>after',
                ])
            );

            const {edges, nodes} = build([buildTask('each_1', 'each/v1', {}), buildTask('after')]);

            expect(nodes.map((node) => node.id)).toContain('each_1-each-placeholder-0');
            expect(edges.map((edge) => edge.id)).toContain('each_1-each-placeholder-0=>each_1-each-bottom-ghost');
        });

        it('wires fork-join branches and adds a ghost lane when fewer than two lanes are filled', () => {
            const filledEdges = edgeIds([
                buildTask('fork_1', 'fork-join/v1', {branches: [[buildTask('x1')], [buildTask('y1')]]}),
                buildTask('x1'),
                buildTask('y1'),
                buildTask('after'),
            ]);

            expect(filledEdges).toEqual(
                expect.arrayContaining([
                    'fork_1-forkJoin-top-ghost=>x1',
                    'x1=>fork_1-forkJoin-bottom-ghost',
                    'fork_1-forkJoin-top-ghost=>y1',
                    'y1=>fork_1-forkJoin-bottom-ghost',
                    'fork_1-forkJoin-bottom-ghost=>after',
                ])
            );

            const emptyNodeIds = nodeIds([
                buildTask('fork_1', 'fork-join/v1', {branches: [[], []]}),
                buildTask('after'),
            ]);

            expect(emptyNodeIds.some((id) => id.startsWith('fork_1-forkJoin-placeholder'))).toBe(true);
        });

        it('connects a dispatcher nested in another dispatcher branch to the outer bottom ghost', () => {
            const nestedLoop = buildTask('loop_1', 'loop/v1', {iteratee: []});

            const {edges} = build([
                buildTask('condition_1', 'condition/v1', {caseFalse: [], caseTrue: [nestedLoop]}),
                nestedLoop,
                buildTask('after'),
            ]);

            expect(edges.map((edge) => edge.id)).toEqual(
                expect.arrayContaining([
                    'condition_1-condition-top-ghost=>loop_1',
                    'loop_1-loop-bottom-ghost=>condition_1-condition-bottom-ghost',
                    'condition_1-condition-bottom-ghost=>after',
                ])
            );
        });

        it('routes a trailing dispatcher bottom ghost to the final placeholder', () => {
            const {edges, nodes} = build([
                buildTask('condition_1', 'condition/v1', {caseFalse: [buildTask('f1')], caseTrue: [buildTask('a1')]}),
                buildTask('a1'),
                buildTask('f1'),
            ]);

            expect(edges.map((edge) => edge.id)).toContain(
                `condition_1-condition-bottom-ghost=>${FINAL_PLACEHOLDER_NODE_ID}`
            );
            expect(nodes.filter((node) => node.id === FINAL_PLACEHOLDER_NODE_ID)).toHaveLength(1);
        });

        it('routes a trailing on-error bottom ghost to the final placeholder', () => {
            const {edges, nodes} = build([
                buildTask('onError_1', 'on-error/v1', {
                    'main-branch': [buildTask('m1')],
                    'on-error-branch': [buildTask('e1')],
                }),
                buildTask('m1'),
                buildTask('e1'),
            ]);

            expect(edges.map((edge) => edge.id)).toContain(
                `onError_1-onError-bottom-ghost=>${FINAL_PLACEHOLDER_NODE_ID}`
            );
            expect(nodes.filter((node) => node.id === FINAL_PLACEHOLDER_NODE_ID)).toHaveLength(1);
        });

        it('uses a component icon node for dispatchers without a definition', () => {
            const {nodes} = build([buildTask('loop_1', 'loop/v1', {iteratee: []})]);

            const loopNode = nodes.find((node) => node.id === 'loop_1');
            const loopData = loopNode?.data as Record<string, unknown>;

            expect(loopData).toMatchObject({componentName: 'loop', taskDispatcher: true, taskDispatcherId: 'loop_1'});
        });

        it('marks cluster root tasks with the clusterRoot node type', () => {
            const clusterRootTask: WorkflowTask = {...buildTask('root_1'), clusterRoot: true};

            const {nodes} = build([clusterRootTask]);

            expect(nodes.find((node) => node.id === 'root_1')?.type).toBe('clusterRoot');
        });

        it('converts a task through its component definition when one is available', () => {
            const {nodes} = buildWorkflowGraph({
                canvasWidth: 800,
                componentDefinitions: [
                    {
                        icon: 'http-icon',
                        name: 'httpClient',
                        title: 'HTTP Client',
                        version: 1,
                    } as ComponentDefinitionBasic,
                ],
                taskDispatcherDefinitions: [],
                tasks: [buildTask('t1')],
                triggers: [],
            });

            const taskNode = nodes.find((node) => node.id === 't1');
            const taskData = taskNode?.data as Record<string, unknown>;

            expect(taskNode?.type).toBe('workflow');
            expect(taskData).toMatchObject({componentName: 'httpClient', name: 't1'});
        });
    });
});
