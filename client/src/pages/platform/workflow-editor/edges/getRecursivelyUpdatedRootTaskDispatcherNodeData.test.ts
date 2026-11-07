import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import getRecursivelyUpdatedRootTaskDispatcherNodeData from './getRecursivelyUpdatedRootTaskDispatcherNodeData';

function buildTask(name: string, parameters: Record<string, unknown> = {}): WorkflowTask {
    return {name, parameters, type: 'mailchimp/v1/subscribe'};
}

function buildNode(nodeData: NodeDataType): Node {
    return {data: nodeData, id: nodeData.name, position: {x: 0, y: 0}, type: 'workflow'};
}

function buildNodeData(task: WorkflowTask, nestingData: Partial<NodeDataType> = {}): NodeDataType {
    return {
        ...nestingData,
        componentName: task.type.split('/')[0],
        name: task.name,
        parameters: task.parameters,
        type: task.type,
        workflowNodeName: task.name,
    } as NodeDataType;
}

function toNodesMap(nodeDataList: NodeDataType[]): Map<string, Node> {
    const nodeEntries = nodeDataList.map((nodeData): [string, Node] => [nodeData.name, buildNode(nodeData)]);

    return new Map(nodeEntries);
}

describe('getRecursivelyUpdatedRootTaskDispatcherNodeData', () => {
    it('returns the node data unchanged when the task is not nested in a task dispatcher', () => {
        const taskNodeData = buildNodeData(buildTask('task_1', {text: 'updated'}));
        const definitionTasks = [buildTask('task_1')];
        const nodesMap = toNodesMap([taskNodeData]);

        const result = getRecursivelyUpdatedRootTaskDispatcherNodeData({
            currentTaskNodeData: taskNodeData,
            definitionTasks,
            nodesMap,
        });

        expect(result).toBe(taskNodeData);
    });

    it('writes a graph member parameter change back into the graph node list', () => {
        const graphTask: WorkflowTask = {
            name: 'graph_1',
            parameters: {nodes: [buildTask('task_1', {text: 'old'}), buildTask('task_2')], transitions: []},
            type: 'graph/v1',
        };

        const graphNodeData = buildNodeData(graphTask);

        const updatedTaskNodeData = buildNodeData(buildTask('task_1', {text: 'updated'}), {
            graphData: {graphId: 'graph_1', index: 0},
        });

        const nodesMap = toNodesMap([graphNodeData, updatedTaskNodeData]);

        const result = getRecursivelyUpdatedRootTaskDispatcherNodeData({
            currentTaskNodeData: updatedTaskNodeData,
            definitionTasks: [graphTask],
            nodesMap,
        });

        const members = result.parameters?.nodes as Array<WorkflowTask>;

        expect(result.name).toBe('graph_1');
        expect(members[0].parameters).toEqual({text: 'updated'});
        expect(members[1]).toEqual(buildTask('task_2'));
    });

    it('walks up through a loop nested in a graph to the root graph', () => {
        const loopTask: WorkflowTask = {
            name: 'loop_1',
            parameters: {items: [], iteratee: [buildTask('task_1', {text: 'old'})]},
            type: 'loop/v1',
        };

        const graphTask: WorkflowTask = {
            name: 'graph_1',
            parameters: {nodes: [loopTask], transitions: []},
            type: 'graph/v1',
        };

        const graphNodeData = buildNodeData(graphTask);
        const loopNodeData = buildNodeData(loopTask, {graphData: {graphId: 'graph_1', index: 0}});

        const updatedTaskNodeData = buildNodeData(buildTask('task_1', {text: 'updated'}), {
            loopData: {index: 0, loopId: 'loop_1'},
        });

        const nodesMap = toNodesMap([graphNodeData, loopNodeData, updatedTaskNodeData]);

        const result = getRecursivelyUpdatedRootTaskDispatcherNodeData({
            currentTaskNodeData: updatedTaskNodeData,
            definitionTasks: [graphTask],
            nodesMap,
        });

        const [updatedLoopTask] = result.parameters?.nodes as Array<WorkflowTask>;

        expect(result.name).toBe('graph_1');
        expect(updatedLoopTask.name).toBe('loop_1');
        expect(updatedLoopTask.parameters?.iteratee).toEqual([buildTask('task_1', {text: 'updated'})]);
    });

    it('returns the node data unchanged when the parent dispatcher has no canvas node', () => {
        const graphTask: WorkflowTask = {
            name: 'graph_1',
            parameters: {nodes: [buildTask('task_1')], transitions: []},
            type: 'graph/v1',
        };

        const updatedTaskNodeData = buildNodeData(buildTask('task_1', {text: 'updated'}), {
            graphData: {graphId: 'graph_1', index: 0},
        });

        const nodesMap = toNodesMap([updatedTaskNodeData]);

        const result = getRecursivelyUpdatedRootTaskDispatcherNodeData({
            currentTaskNodeData: updatedTaskNodeData,
            definitionTasks: [graphTask],
            nodesMap,
        });

        expect(result).toBe(updatedTaskNodeData);
    });

    it('returns the node data unchanged when the task is not one of the dispatcher subtasks', () => {
        const graphTask: WorkflowTask = {
            name: 'graph_1',
            parameters: {nodes: [buildTask('task_2')], transitions: []},
            type: 'graph/v1',
        };

        const updatedTaskNodeData = buildNodeData(buildTask('task_1', {text: 'updated'}), {
            graphData: {graphId: 'graph_1', index: 0},
        });

        const graphNodeData = buildNodeData(graphTask);
        const nodesMap = toNodesMap([graphNodeData, updatedTaskNodeData]);

        const result = getRecursivelyUpdatedRootTaskDispatcherNodeData({
            currentTaskNodeData: updatedTaskNodeData,
            definitionTasks: [graphTask],
            nodesMap,
        });

        expect(result).toBe(updatedTaskNodeData);
    });
});
