import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    getReadOnlyFramedClusterRootIds,
    toReadOnlyClusterFrameMembers,
    toReadOnlyFramedClusterRoot,
} from './readOnlyClusterFrames';

const TASKS = [
    {clusterRoot: true, name: 'aiAgent_1', type: 'aiAgent/v1/chat'},
    {name: 'var_1', type: 'var/v1/set'},
];

describe('readOnlyClusterFrames', () => {
    it('frames every cluster root on a read-only canvas with an execution overlay', () => {
        expect(getReadOnlyFramedClusterRootIds(TASKS, true)).toEqual(['aiAgent_1']);
    });

    it('frames nothing on a read-only canvas without an execution overlay', () => {
        expect(getReadOnlyFramedClusterRootIds(TASKS, false)).toEqual([]);
    });

    it('drops placeholder members and the edges that lead to them', () => {
        const memberNodes = [
            {data: {}, id: 'httpClient_1', position: {x: 0, y: 0}, type: 'workflow'},
            {data: {}, id: 'aiAgent_1-memory-placeholder-0', position: {x: 0, y: 0}, type: 'placeholder'},
        ] as Node[];
        const memberEdges = [
            {id: 'aiAgent_1=>aiAgent_1-httpClient_1', source: 'aiAgent_1', target: 'httpClient_1'},
            {
                id: 'aiAgent_1=>aiAgent_1-memory-placeholder-0',
                source: 'aiAgent_1',
                target: 'aiAgent_1-memory-placeholder-0',
            },
        ] as Edge[];

        const result = toReadOnlyClusterFrameMembers(memberNodes, memberEdges);

        expect(result.memberNodes.map((node) => node.id)).toEqual(['httpClient_1']);
        expect(result.memberEdges.map((edge) => edge.id)).toEqual(['aiAgent_1=>aiAgent_1-httpClient_1']);
    });

    it('makes members non-interactive', () => {
        const [memberNode] = toReadOnlyClusterFrameMembers(
            [{className: 'existing', data: {}, id: 'httpClient_1', position: {x: 0, y: 0}, type: 'workflow'}] as Node[],
            []
        ).memberNodes;

        expect(memberNode).toMatchObject({
            connectable: false,
            deletable: false,
            draggable: false,
            focusable: false,
            selectable: false,
            type: 'readonlyClusterElement',
        });
        expect(memberNode.className).toContain('pointer-events-none');
        expect(memberNode.className).toContain('existing');
    });

    it('keeps a framed root its own type and makes it non-interactive', () => {
        const root = toReadOnlyFramedClusterRoot(
            {
                data: {},
                id: 'aiAgent_1',
                position: {x: 0, y: 0},
                type: 'clusterRoot',
            } as Node,
            new Set()
        );

        expect(root).toMatchObject({draggable: false, selectable: false, type: 'clusterRoot'});
        expect(root.className).toContain('pointer-events-none');
    });

    it('mutes a framed root that is disabled itself or through an ancestor', () => {
        const node = {data: {workflowNodeName: 'aiAgent_1'}, id: 'aiAgent_1', position: {x: 0, y: 0}} as Node;

        expect(toReadOnlyFramedClusterRoot(node, new Set()).data.isEffectivelyDisabled).toBe(false);
        expect(toReadOnlyFramedClusterRoot(node, new Set(['aiAgent_1'])).data.isEffectivelyDisabled).toBe(true);
        expect(
            toReadOnlyFramedClusterRoot({...node, data: {...node.data, disabled: true}}, new Set()).data
                .isEffectivelyDisabled
        ).toBe(true);
    });
});
