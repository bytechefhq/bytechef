import {type Edge, type Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {toConditionCaseKey} from './collectTakenEmptyConditionCases';
import getExecutedEdgeStatus, {
    bypassDisabledEdgeEndpoints,
    isConditionPlaceholderNode,
    resolveTestStateNodeName,
} from './getExecutedEdgeStatus';

const taskNode = (workflowNodeName: string): Node => ({
    data: {workflowNodeName},
    id: workflowNodeName,
    position: {x: 0, y: 0},
});

const ghostNode = (taskDispatcherId: string, suffix: string): Node => ({
    data: {taskDispatcherId},
    id: `${taskDispatcherId}-${suffix}`,
    position: {x: 0, y: 0},
    type: 'taskDispatcherTopGhostNode',
});

const bottomGhostNode = (taskDispatcherId: string, suffix: string): Node => ({
    data: {taskDispatcherId},
    id: `${taskDispatcherId}-${suffix}`,
    position: {x: 0, y: 0},
    type: 'taskDispatcherBottomGhostNode',
});

const leftGhostNode = (taskDispatcherId: string): Node => ({
    data: {taskDispatcherId},
    id: `${taskDispatcherId}-taskDispatcher-left-ghost`,
    position: {x: 0, y: 0},
    type: 'taskDispatcherLeftGhostNode',
});

const dispatcherNode = (workflowNodeName: string): Node => ({
    data: {taskDispatcher: true, workflowNodeName},
    id: workflowNodeName,
    position: {x: 0, y: 0},
});

const disabledTaskNode = (workflowNodeName: string): Node => ({
    data: {disabled: true, workflowNodeName},
    id: workflowNodeName,
    position: {x: 0, y: 0},
});

const triggerNode = (workflowNodeName: string): Node => ({
    data: {trigger: true, workflowNodeName},
    id: workflowNodeName,
    position: {x: 0, y: 0},
});

const chainEdge = (source: string, target: string): Edge => ({id: `${source}=>${target}`, source, target});

describe('resolveTestStateNodeName', () => {
    it('prefers workflowNodeName, falls back to taskDispatcherId, then id', () => {
        expect(resolveTestStateNodeName(taskNode('logger_1'))).toBe('logger_1');
        expect(resolveTestStateNodeName(ghostNode('condition_1', 'condition-top-ghost'))).toBe('condition_1');
        expect(resolveTestStateNodeName({data: {}, id: 'raw-id', position: {x: 0, y: 0}})).toBe('raw-id');
        expect(resolveTestStateNodeName(undefined)).toBeUndefined();
    });
});

describe('getExecutedEdgeStatus', () => {
    it('marks an edge COMPLETED when both endpoints completed', () => {
        const states = {logger_1: {status: 'COMPLETED' as const}, var_1: {status: 'COMPLETED' as const}};
        const sourceNode = taskNode('var_1');
        const targetNode = taskNode('logger_1');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBe('COMPLETED');
    });

    it('marks the incoming edge of a failed node COMPLETED, since the data reached it', () => {
        const states = {http_1: {status: 'FAILED' as const}, var_1: {status: 'COMPLETED' as const}};
        const sourceNode = taskNode('var_1');
        const targetNode = taskNode('http_1');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBe('COMPLETED');
    });

    it('never colors the edges into or out of a "+" placeholder', () => {
        const placeholderNode: Node = {
            data: {taskDispatcherId: 'parallel_1'},
            id: 'parallel_1-parallel-placeholder-0',
            position: {x: 0, y: 0},
            type: 'placeholder',
        };

        const states = {parallel_1: {status: 'COMPLETED' as const}};
        const topGhost = ghostNode('parallel_1', 'parallel-top-ghost');
        const bottomGhost = ghostNode('parallel_1', 'parallel-bottom-ghost');

        expect(
            getExecutedEdgeStatus({sourceNode: topGhost, targetNode: placeholderNode, workflowTestNodeStates: states})
        ).toBeUndefined();

        expect(
            getExecutedEdgeStatus({
                sourceNode: placeholderNode,
                targetNode: bottomGhost,
                workflowTestNodeStates: states,
            })
        ).toBeUndefined();
    });

    it('colors the edges around a condition placeholder only when the run took that empty case', () => {
        const conditionPlaceholderNode = (conditionCase: string, side: string): Node => ({
            data: {conditionCase, conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: `condition_1-condition-${side}-placeholder-0`,
            position: {x: 0, y: 0},
            type: 'placeholder',
        });

        const states = {condition_1: {status: 'COMPLETED' as const}};
        const takenEmptyConditionCases = new Set([toConditionCaseKey('condition_1', 'caseFalse')]);

        const topGhost = ghostNode('condition_1', 'condition-top-ghost');
        const bottomGhost = bottomGhostNode('condition_1', 'condition-bottom-ghost');
        const takenPlaceholder = conditionPlaceholderNode('caseFalse', 'right');
        const untakenPlaceholder = conditionPlaceholderNode('caseTrue', 'left');

        expect(
            getExecutedEdgeStatus({
                sourceNode: topGhost,
                takenEmptyConditionCases,
                targetNode: takenPlaceholder,
                workflowTestNodeStates: states,
            })
        ).toBe('COMPLETED');
        expect(
            getExecutedEdgeStatus({
                sourceNode: takenPlaceholder,
                takenEmptyConditionCases,
                targetNode: bottomGhost,
                workflowTestNodeStates: states,
            })
        ).toBe('COMPLETED');
        expect(
            getExecutedEdgeStatus({
                sourceNode: topGhost,
                takenEmptyConditionCases,
                targetNode: untakenPlaceholder,
                workflowTestNodeStates: states,
            })
        ).toBeUndefined();
        expect(
            getExecutedEdgeStatus({
                sourceNode: untakenPlaceholder,
                takenEmptyConditionCases,
                targetNode: bottomGhost,
                workflowTestNodeStates: states,
            })
        ).toBeUndefined();
    });

    it('treats a read-only condition placeholder like an editable one', () => {
        const readonlyConditionPlaceholderNode = (conditionCase: string, side: string): Node => ({
            data: {conditionCase, conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: `condition_1-condition-${side}-placeholder-0`,
            position: {x: 0, y: 0},
            type: 'readonlyPlaceholder',
        });

        const states = {condition_1: {status: 'COMPLETED' as const}};
        const takenEmptyConditionCases = new Set([toConditionCaseKey('condition_1', 'caseFalse')]);

        const topGhost = ghostNode('condition_1', 'condition-top-ghost');
        const takenPlaceholder = readonlyConditionPlaceholderNode('caseFalse', 'right');
        const untakenPlaceholder = readonlyConditionPlaceholderNode('caseTrue', 'left');

        expect(isConditionPlaceholderNode(untakenPlaceholder)).toBe(true);
        expect(
            getExecutedEdgeStatus({
                sourceNode: topGhost,
                takenEmptyConditionCases,
                targetNode: takenPlaceholder,
                workflowTestNodeStates: states,
            })
        ).toBe('COMPLETED');
        expect(
            getExecutedEdgeStatus({
                sourceNode: topGhost,
                takenEmptyConditionCases,
                targetNode: untakenPlaceholder,
                workflowTestNodeStates: states,
            })
        ).toBeUndefined();
    });

    it('leaves the stem into a condition with two empty cases neutral, since the taken side is unknown', () => {
        const conditionNode = (caseTrue: unknown[], caseFalse: unknown[]): Node => ({
            data: {parameters: {caseFalse, caseTrue}, workflowNodeName: 'condition_2'},
            id: 'condition_2',
            position: {x: 0, y: 0},
        });

        const states = {condition_2: {status: 'COMPLETED' as const}};
        const topGhost = ghostNode('condition_2', 'condition-top-ghost');
        const emptyConditionNode = conditionNode([], []);
        const filledConditionNode = conditionNode([{name: 'logger_1'}], []);

        expect(
            getExecutedEdgeStatus({
                sourceNode: emptyConditionNode,
                targetNode: topGhost,
                workflowTestNodeStates: states,
            })
        ).toBeUndefined();
        expect(
            getExecutedEdgeStatus({
                sourceNode: filledConditionNode,
                targetNode: topGhost,
                workflowTestNodeStates: states,
            })
        ).toBe('COMPLETED');
    });

    it('colors dispatcher plumbing through ghost nodes', () => {
        const states = {condition_1: {status: 'COMPLETED' as const}, logger_3: {status: 'COMPLETED' as const}};
        const sourceNode = ghostNode('condition_1', 'condition-top-ghost');
        const targetNode = taskNode('logger_3');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBe('COMPLETED');
    });

    it('colors plumbing out of a dispatcher that failed through a nested child', () => {
        const states = {condition_1: {status: 'FAILED' as const}, condition_2: {status: 'FAILED' as const}};
        const outerConditionNode = taskNode('condition_1');
        const topGhost = ghostNode('condition_1', 'condition-top-ghost');
        const nestedConditionNode = taskNode('condition_2');

        expect(
            getExecutedEdgeStatus({
                sourceNode: outerConditionNode,
                targetNode: topGhost,
                workflowTestNodeStates: states,
            })
        ).toBe('COMPLETED');

        expect(
            getExecutedEdgeStatus({
                sourceNode: topGhost,
                targetNode: nestedConditionNode,
                workflowTestNodeStates: states,
            })
        ).toBe('COMPLETED');
    });

    it('keeps the sibling branch of a failed dispatcher gray', () => {
        const states = {condition_1: {status: 'FAILED' as const}, condition_2: {status: 'FAILED' as const}};
        const sourceNode = ghostNode('condition_1', 'condition-top-ghost');
        const targetNode = taskNode('logger_2');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBeUndefined();
    });

    it('colors a completed child of a dispatcher that failed later in the same branch', () => {
        const states = {condition_1: {status: 'FAILED' as const}, logger_5: {status: 'COMPLETED' as const}};
        const sourceNode = ghostNode('condition_1', 'condition-top-ghost');
        const targetNode = taskNode('logger_5');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBe('COMPLETED');
    });

    it('keeps the untaken branch gray: child without state into an executed ghost', () => {
        const states = {condition_1: {status: 'COMPLETED' as const}};
        const sourceNode = taskNode('logger_2');
        const targetNode = ghostNode('condition_1', 'condition-bottom-ghost');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBeUndefined();
    });

    it('colors a completed branch into the join green even when a sibling branch failed the dispatcher', () => {
        const states = {dataStorage_2: {status: 'COMPLETED' as const}, parallel_1: {status: 'FAILED' as const}};
        const sourceNode = taskNode('dataStorage_2');
        const targetNode = bottomGhostNode('parallel_1', 'parallel-bottom-ghost');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBe('COMPLETED');
    });

    it('keeps the edge from a failed last child into the join neutral', () => {
        const states = {http_1: {status: 'FAILED' as const}, parallel_1: {status: 'FAILED' as const}};
        const sourceNode = taskNode('http_1');
        const targetNode = bottomGhostNode('parallel_1', 'parallel-bottom-ghost');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBeUndefined();
    });

    it('keeps the edge from a failed nested dispatcher into the outer join neutral', () => {
        const states = {condition_2: {status: 'FAILED' as const}, parallel_1: {status: 'FAILED' as const}};
        const sourceNode = bottomGhostNode('condition_2', 'condition-bottom-ghost');
        const targetNode = bottomGhostNode('parallel_1', 'parallel-bottom-ghost');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBeUndefined();
    });

    it('colors only the edge out of the trigger that started the run', () => {
        const states = {trigger_2: {status: 'COMPLETED' as const}, var_1: {status: 'COMPLETED' as const}};
        const startingTriggerNode = triggerNode('trigger_2');
        const idleTriggerNode = triggerNode('trigger_1');
        const varNode = taskNode('var_1');

        expect(
            getExecutedEdgeStatus({
                sourceNode: startingTriggerNode,
                targetNode: varNode,
                workflowTestNodeStates: states,
            })
        ).toBe('COMPLETED');
        expect(
            getExecutedEdgeStatus({sourceNode: idleTriggerNode, targetNode: varNode, workflowTestNodeStates: states})
        ).toBeUndefined();
    });

    it('colors the way into a running loop up to its completed child', () => {
        const states = {
            logger_1: {status: 'COMPLETED' as const},
            loop_2: {status: 'RUNNING' as const},
            var_1: {status: 'COMPLETED' as const},
        };

        const topGhost = ghostNode('loop_2', 'loop-top-ghost');
        const varNode = taskNode('var_1');
        const loopNode = dispatcherNode('loop_2');
        const loggerNode = taskNode('logger_1');

        expect(getExecutedEdgeStatus({sourceNode: varNode, targetNode: loopNode, workflowTestNodeStates: states})).toBe(
            'COMPLETED'
        );
        expect(
            getExecutedEdgeStatus({sourceNode: loopNode, targetNode: topGhost, workflowTestNodeStates: states})
        ).toBe('COMPLETED');
        expect(
            getExecutedEdgeStatus({sourceNode: topGhost, targetNode: loggerNode, workflowTestNodeStates: states})
        ).toBe('COMPLETED');
    });

    it('keeps the edges of a running loop neutral where the run has not reached yet', () => {
        const states = {
            logger_1: {status: 'RUNNING' as const},
            loop_2: {status: 'RUNNING' as const},
        };

        const topGhost = ghostNode('loop_2', 'loop-top-ghost');
        const runningLoggerNode = taskNode('logger_1');
        const unreachedLoggerNode = taskNode('logger_2');
        const innerBottomGhost = bottomGhostNode('loop_2', 'loop-bottom-ghost');
        const outerBottomGhost = bottomGhostNode('loop_1', 'loop-bottom-ghost');

        expect(
            getExecutedEdgeStatus({sourceNode: topGhost, targetNode: runningLoggerNode, workflowTestNodeStates: states})
        ).toBeUndefined();
        expect(
            getExecutedEdgeStatus({
                sourceNode: topGhost,
                targetNode: unreachedLoggerNode,
                workflowTestNodeStates: states,
            })
        ).toBeUndefined();
        expect(
            getExecutedEdgeStatus({
                sourceNode: innerBottomGhost,
                targetNode: outerBottomGhost,
                workflowTestNodeStates: states,
            })
        ).toBeUndefined();
    });

    it('colors the left rail of a running loop', () => {
        const states = {loop_2: {status: 'RUNNING' as const}};

        const leftGhost = leftGhostNode('loop_2');
        const topGhost = ghostNode('loop_2', 'loop-top-ghost');
        const bottomGhost = bottomGhostNode('loop_2', 'loop-bottom-ghost');

        expect(
            getExecutedEdgeStatus({sourceNode: topGhost, targetNode: leftGhost, workflowTestNodeStates: states})
        ).toBe('COMPLETED');
        expect(
            getExecutedEdgeStatus({sourceNode: leftGhost, targetNode: bottomGhost, workflowTestNodeStates: states})
        ).toBe('COMPLETED');
    });

    it('keeps the left rail of a loop the run has not reached neutral', () => {
        const leftGhost = leftGhostNode('loop_2');
        const topGhost = ghostNode('loop_2', 'loop-top-ghost');
        const bottomGhost = bottomGhostNode('loop_2', 'loop-bottom-ghost');

        expect(
            getExecutedEdgeStatus({sourceNode: topGhost, targetNode: leftGhost, workflowTestNodeStates: {}})
        ).toBeUndefined();
        expect(
            getExecutedEdgeStatus({sourceNode: leftGhost, targetNode: bottomGhost, workflowTestNodeStates: {}})
        ).toBeUndefined();
    });

    it('colors the way into a running dispatcher nested in a running loop', () => {
        const states = {
            condition_1: {status: 'RUNNING' as const},
            loop_2: {status: 'RUNNING' as const},
        };

        const sourceNode = ghostNode('loop_2', 'loop-top-ghost');
        const targetNode = dispatcherNode('condition_1');

        expect(getExecutedEdgeStatus({sourceNode, targetNode, workflowTestNodeStates: states})).toBe('COMPLETED');
    });

    it('is neutral while the target is still running or has no state', () => {
        const states = {logger_1: {status: 'RUNNING' as const}, var_1: {status: 'COMPLETED' as const}};
        const varNode = taskNode('var_1');
        const runningLoggerNode = taskNode('logger_1');
        const missingNode = taskNode('missing');

        expect(
            getExecutedEdgeStatus({sourceNode: varNode, targetNode: runningLoggerNode, workflowTestNodeStates: states})
        ).toBeUndefined();
        expect(
            getExecutedEdgeStatus({sourceNode: varNode, targetNode: missingNode, workflowTestNodeStates: states})
        ).toBeUndefined();
    });
});

describe('bypassDisabledEdgeEndpoints', () => {
    const nodes = [
        ghostNode('parallel_1', 'parallel-top-ghost'),
        disabledTaskNode('firecrawl_4'),
        taskNode('firecrawl_7'),
        disabledTaskNode('openAi_2'),
        taskNode('anthropic_1'),
    ];

    const edges = [
        chainEdge('parallel_1-parallel-top-ghost', 'firecrawl_4'),
        chainEdge('firecrawl_4', 'firecrawl_7'),
        chainEdge('firecrawl_7', 'openAi_2'),
        chainEdge('openAi_2', 'anthropic_1'),
    ];

    const states = {
        anthropic_1: {status: 'FAILED' as const},
        firecrawl_7: {status: 'COMPLETED' as const},
        parallel_1: {status: 'FAILED' as const},
    };

    const statusOf = (source: string, target: string, disabledTaskNames = new Set<string>()) => {
        const endpoints = bypassDisabledEdgeEndpoints(
            nodes.find((node) => node.id === source),
            nodes.find((node) => node.id === target),
            {disabledTaskNames, edges, nodes}
        );

        return getExecutedEdgeStatus({
            sourceNode: endpoints.sourceNode,
            targetNode: endpoints.targetNode,
            workflowTestNodeStates: states,
        });
    };

    it('carries the executed trail across a disabled node up to the failed node', () => {
        expect(statusOf('firecrawl_7', 'openAi_2')).toBe('COMPLETED');
        expect(statusOf('openAi_2', 'anthropic_1')).toBe('COMPLETED');
    });

    it('carries the executed path from a ghost across a disabled first child', () => {
        expect(statusOf('parallel_1-parallel-top-ghost', 'firecrawl_4')).toBe('COMPLETED');
        expect(statusOf('firecrawl_4', 'firecrawl_7')).toBe('COMPLETED');
    });

    it('treats a task under a disabled ancestor as disabled', () => {
        const ancestorNodes = [taskNode('firecrawl_7'), taskNode('openAi_2'), taskNode('anthropic_1')];

        const endpoints = bypassDisabledEdgeEndpoints(ancestorNodes[0], ancestorNodes[1], {
            disabledTaskNames: new Set(['openAi_2']),
            edges,
            nodes: ancestorNodes,
        });

        expect(endpoints.targetNode?.id).toBe('anthropic_1');
    });

    it('stops at a disabled node that fans out, since no single path runs through it', () => {
        const fanOutEdges = [...edges, chainEdge('openAi_2', 'firecrawl_4')];

        const endpoints = bypassDisabledEdgeEndpoints(nodes[2], nodes[3], {
            disabledTaskNames: new Set(),
            edges: fanOutEdges,
            nodes,
        });

        expect(endpoints.targetNode?.id).toBe('openAi_2');
        expect(
            getExecutedEdgeStatus({
                sourceNode: endpoints.sourceNode,
                targetNode: endpoints.targetNode,
                workflowTestNodeStates: states,
            })
        ).toBeUndefined();
    });

    it('keeps an untaken branch neutral across a disabled node', () => {
        const endpoints = bypassDisabledEdgeEndpoints(nodes[0], nodes[1], {disabledTaskNames: new Set(), edges, nodes});

        const states = {parallel_1: {status: 'COMPLETED' as const}};

        expect(
            getExecutedEdgeStatus({
                sourceNode: endpoints.sourceNode,
                targetNode: endpoints.targetNode,
                workflowTestNodeStates: states,
            })
        ).toBeUndefined();
    });

    it('returns enabled endpoints untouched', () => {
        const endpoints = bypassDisabledEdgeEndpoints(nodes[2], nodes[4], {disabledTaskNames: new Set(), edges, nodes});

        expect(endpoints).toEqual({sourceNode: nodes[2], targetNode: nodes[4]});
    });
});

describe('getExecutedEdgeStatus graph transition arm', () => {
    interface NodeExecutionProps {
        nodeName: string;
        startedAtSecond: number;
        status?: string;
    }

    const nodeExecution = ({nodeName, startedAtSecond, status = 'COMPLETED'}: NodeExecutionProps) => ({
        nodeName,
        startDate: new Date(`2024-01-01T10:00:${String(startedAtSecond).padStart(2, '0')}`),
        status,
    });

    const fanOutExecutions = [
        nodeExecution({nodeName: 'node_a', startedAtSecond: 1}),
        nodeExecution({nodeName: 'node_b', startedAtSecond: 2}),
        nodeExecution({nodeName: 'node_c', startedAtSecond: 3}),
    ];

    it('marks a transition COMPLETED when its target was dispatched right after its source', () => {
        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [fanOutExecutions],
            to: 'node_b',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBe('COMPLETED');
    });

    it('leaves an untaken sibling transition neutral even though its target completed later', () => {
        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [fanOutExecutions],
            to: 'node_c',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBeUndefined();
    });

    it('orders executions by startDate rather than trusting the order it was handed', () => {
        const nodeExecutions = [
            nodeExecution({nodeName: 'node_c', startedAtSecond: 3}),
            nodeExecution({nodeName: 'node_b', startedAtSecond: 2}),
            nodeExecution({nodeName: 'node_a', startedAtSecond: 1}),
        ];

        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [nodeExecutions],
            to: 'node_b',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBe('COMPLETED');
    });

    it('marks a self transition COMPLETED across consecutive visits of one node', () => {
        const nodeExecutions = [
            nodeExecution({nodeName: 'node_a', startedAtSecond: 1}),
            nodeExecution({nodeName: 'node_a', startedAtSecond: 2}),
        ];

        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [nodeExecutions],
            to: 'node_a',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBe('COMPLETED');
    });

    it('paints a transition FAILED when the visit it led to failed', () => {
        const nodeExecutions = [
            nodeExecution({nodeName: 'node_a', startedAtSecond: 1}),
            nodeExecution({nodeName: 'node_b', startedAtSecond: 2, status: 'FAILED'}),
        ];

        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [nodeExecutions],
            to: 'node_b',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBe('FAILED');
    });

    it('prefers FAILED over COMPLETED when a cycle took the same transition both ways', () => {
        const nodeExecutions = [
            nodeExecution({nodeName: 'node_a', startedAtSecond: 1}),
            nodeExecution({nodeName: 'node_b', startedAtSecond: 2}),
            nodeExecution({nodeName: 'node_a', startedAtSecond: 3}),
            nodeExecution({nodeName: 'node_b', startedAtSecond: 4, status: 'FAILED'}),
        ];

        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [nodeExecutions],
            to: 'node_b',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBe('FAILED');
    });

    it('stays neutral while the visit it led to is still running', () => {
        const nodeExecutions = [
            nodeExecution({nodeName: 'node_a', startedAtSecond: 1}),
            nodeExecution({nodeName: 'node_b', startedAtSecond: 2, status: 'STARTED'}),
        ];

        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [nodeExecutions],
            to: 'node_b',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBeUndefined();
    });

    it('marks a dynamic transition COMPLETED when the next visit is a node no static transition names', () => {
        const graphTransition = {
            dynamic: true,
            from: 'node_a',
            nodeExecutionRuns: [fanOutExecutions],
            staticTargetNames: ['node_c'],
            to: "=nextStep + '_1'",
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBe('COMPLETED');
    });

    it('leaves a dynamic transition neutral when the next visit is a declared static target', () => {
        const graphTransition = {
            dynamic: true,
            from: 'node_a',
            nodeExecutionRuns: [fanOutExecutions],
            staticTargetNames: ['node_b'],
            to: "=nextStep + '_1'",
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBeUndefined();
    });

    it('paints a dynamic transition FAILED when the visit it resolved to failed', () => {
        const nodeExecutions = [
            nodeExecution({nodeName: 'node_a', startedAtSecond: 1}),
            nodeExecution({nodeName: 'node_b', startedAtSecond: 2, status: 'FAILED'}),
        ];

        const graphTransition = {
            dynamic: true,
            from: 'node_a',
            nodeExecutionRuns: [nodeExecutions],
            staticTargetNames: [],
            to: '=${node_a.decision}',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBe('FAILED');
    });

    it('marks a transition COMPLETED when only a later run of the graph took it', () => {
        const firstRun = [
            nodeExecution({nodeName: 'node_a', startedAtSecond: 1}),
            nodeExecution({nodeName: 'node_c', startedAtSecond: 2}),
        ];
        const secondRun = [
            nodeExecution({nodeName: 'node_a', startedAtSecond: 3}),
            nodeExecution({nodeName: 'node_b', startedAtSecond: 4}),
        ];

        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [firstRun, secondRun],
            to: 'node_b',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBe('COMPLETED');
    });

    it('never pairs the last visit of one run with the first visit of the next', () => {
        const firstRun = [nodeExecution({nodeName: 'node_b', startedAtSecond: 1})];
        const secondRun = [nodeExecution({nodeName: 'node_a', startedAtSecond: 2})];

        const graphTransition = {
            from: 'node_b',
            nodeExecutionRuns: [firstRun, secondRun],
            to: 'node_a',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBeUndefined();
    });

    it('prefers FAILED when one run failed on a transition another run completed', () => {
        const firstRun = [
            nodeExecution({nodeName: 'node_a', startedAtSecond: 1}),
            nodeExecution({nodeName: 'node_b', startedAtSecond: 2, status: 'FAILED'}),
        ];
        const secondRun = [
            nodeExecution({nodeName: 'node_a', startedAtSecond: 3}),
            nodeExecution({nodeName: 'node_b', startedAtSecond: 4}),
        ];

        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [firstRun, secondRun],
            to: 'node_b',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBe('FAILED');
    });

    it('stays neutral when an endpoint has no name, which matches no dispatch', () => {
        const namelessExecutions = [
            nodeExecution({nodeName: '', startedAtSecond: 1}),
            nodeExecution({nodeName: '', startedAtSecond: 2}),
        ];

        const graphTransition = {
            from: '',
            nodeExecutionRuns: [namelessExecutions],
            to: '',
        };

        expect(getExecutedEdgeStatus({graphTransition, workflowTestNodeStates: {}})).toBeUndefined();
    });

    it('never consults node states in the graph arm, so a completed pair alone proves nothing', () => {
        const states = {node_a: {status: 'COMPLETED' as const}, node_c: {status: 'COMPLETED' as const}};

        const sourceNode = taskNode('node_a');
        const targetNode = taskNode('node_c');

        const graphTransition = {
            from: 'node_a',
            nodeExecutionRuns: [fanOutExecutions],
            to: 'node_c',
        };

        expect(
            getExecutedEdgeStatus({graphTransition, sourceNode, targetNode, workflowTestNodeStates: states})
        ).toBeUndefined();
    });
});
