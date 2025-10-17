import {describe, expect, it} from 'vitest';

import getTaskDispatcherContext, {
    getContextFromPlaceholderNode,
    getContextFromTaskNodeData,
} from '../utils/getTaskDispatcherContext';

import type {NodeDataType} from '@/shared/types';
import type {Edge, Node} from '@xyflow/react';

// Helper for minimal Node
const makeNode = (props: Partial<Node>): Node => ({
    data: {},
    id: 'node',
    position: {x: 0, y: 0},
    type: 'workflow',
    ...props,
});

// Helper for minimal NodeDataType
const makeNodeData = (props: Partial<NodeDataType>): NodeDataType => ({
    componentName: 'test',
    name: 'test',
    workflowNodeName: 'test',
    ...props,
});

describe('getContextFromTaskNodeData', () => {
    it('should extract context for conditionData', () => {
        const nodeData: NodeDataType = makeNodeData({
            conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 2},
            taskDispatcherId: 'condition_1',
        });

        expect(getContextFromTaskNodeData(nodeData)).toEqual({
            conditionCase: 'caseTrue',
            conditionId: 'condition_1',
            index: 2,
            taskDispatcherId: 'condition_1',
        });
    });

    it('should extract context for loopData', () => {
        const nodeData: NodeDataType = makeNodeData({
            loopData: {index: 1, loopId: 'loop_1'},
            taskDispatcherId: 'loop_1',
        });

        expect(getContextFromTaskNodeData(nodeData)).toEqual({
            index: 1,
            loopId: 'loop_1',
            taskDispatcherId: 'loop_1',
        });
    });

    it('should extract context for branchData', () => {
        const nodeData: NodeDataType = makeNodeData({
            branchData: {branchId: 'branch_1', caseKey: 'case_0', index: 0},
            taskDispatcherId: 'branch_1',
        });

        expect(getContextFromTaskNodeData(nodeData)).toEqual({
            branchId: 'branch_1',
            caseKey: 'case_0',
            index: 0,
            taskDispatcherId: 'branch_1',
        });
    });

    it('should extract context for parallelData', () => {
        const nodeData: NodeDataType = makeNodeData({
            parallelData: {index: 3, parallelId: 'parallel_1'},
            taskDispatcherId: 'parallel_1',
        });

        expect(getContextFromTaskNodeData(nodeData)).toEqual({
            index: 3,
            parallelId: 'parallel_1',
            taskDispatcherId: 'parallel_1',
        });
    });

    it('should extract context for eachData', () => {
        const nodeData: NodeDataType = makeNodeData({
            eachData: {eachId: 'each_1', index: 0},
            taskDispatcherId: 'each_1',
        });

        expect(getContextFromTaskNodeData(nodeData)).toEqual({
            eachId: 'each_1',
            index: 0,
            taskDispatcherId: 'each_1',
        });
    });

    it('should return undefined for empty nodeData', () => {
        expect(getContextFromTaskNodeData(makeNodeData({}))).toEqual({taskDispatcherId: undefined});
    });
});

describe('getContextFromPlaceholderNode', () => {
    function makePlaceholderNode(id: string, data: Record<string, unknown> = {}) {
        return makeNode({data, id, type: 'placeholder'});
    }

    it('should extract context for loop placeholder', () => {
        const node = makePlaceholderNode('loop_1-loop-placeholder-2', {loopId: 'loop_1'});

        expect(getContextFromPlaceholderNode(node)).toMatchObject({
            index: 2,
            loopId: 'loop_1',
            taskDispatcherId: 'loop_1',
        });
    });

    it('should extract context for condition placeholder', () => {
        const node = makePlaceholderNode('condition_1-condition-left-placeholder-0', {
            conditionCase: 'caseTrue',
            conditionId: 'condition_1',
        });

        expect(getContextFromPlaceholderNode(node)).toMatchObject({
            conditionCase: 'caseTrue',
            conditionId: 'condition_1',
            index: 0,
            taskDispatcherId: 'condition_1',
        });
    });

    it('should extract context for branch placeholder', () => {
        const node = makePlaceholderNode('branch_1-branch-case_0-placeholder-1', {
            branchId: 'branch_1',
            caseKey: 'case_0',
        });

        expect(getContextFromPlaceholderNode(node)).toMatchObject({
            branchId: 'branch_1',
            caseKey: 'case_0',
            index: 1,
            taskDispatcherId: 'branch_1',
        });
    });

    it('should extract context for parallel placeholder', () => {
        const node = makePlaceholderNode('parallel_1-parallel-placeholder-4', {parallelId: 'parallel_1'});

        expect(getContextFromPlaceholderNode(node)).toMatchObject({
            index: 4,
            parallelId: 'parallel_1',
            taskDispatcherId: 'parallel_1',
        });
    });

    it('should extract context for each placeholder', () => {
        const node = makePlaceholderNode('each_1-each-placeholder-0', {eachId: 'each_1'});

        expect(getContextFromPlaceholderNode(node)).toMatchObject({
            eachId: 'each_1',
            index: 0,
            taskDispatcherId: 'each_1',
        });
    });

    it('should fallback to taskDispatcherId and index for unknown placeholder', () => {
        const node = makePlaceholderNode('foo-bar-placeholder-7', {taskDispatcherId: 'foo'});

        expect(getContextFromPlaceholderNode(node)).toMatchObject({
            index: 7,
            taskDispatcherId: 'foo',
        });
    });
});

describe('getTaskDispatcherContext', () => {
    it('should return context from placeholder node', () => {
        const node = makeNode({
            data: {conditionCase: 'caseTrue', conditionId: 'condition_1'},
            id: 'condition_1-condition-left-placeholder-0',
            type: 'placeholder',
        });

        expect(getTaskDispatcherContext({node})).toEqual(getContextFromPlaceholderNode(node));
    });

    it('should return undefined if no nodes are provided for edge', () => {
        const edge: Edge = {data: {}, id: 'edge', source: 'source', target: 'target'};

        expect(getTaskDispatcherContext({edge, nodes: undefined})).toBeUndefined();
    });

    it('should return context for workflow-to-task edge', () => {
        const sourceNode = makeNode({
            data: {branchData: {branchId: 'foo', caseKey: 'case_0', index: 0}, taskDispatcherId: 'foo'},
            id: 'source',
        });

        const targetNode = makeNode({
            data: {branchData: {branchId: 'bar', caseKey: 'case_1', index: 1}, taskDispatcherId: 'bar'},
            id: 'target',
        });

        const edge: Edge = {data: {}, id: 'edge', source: 'source', target: 'target'};

        expect(getTaskDispatcherContext({edge, nodes: [sourceNode, targetNode]})).toMatchObject({
            branchId: 'foo',
            caseKey: 'case_0',
            index: 1,
            taskDispatcherId: 'foo',
        });
    });
});
