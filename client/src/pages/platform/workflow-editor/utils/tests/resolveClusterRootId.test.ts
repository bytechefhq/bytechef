import {NodeDataType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {resolveClusterRootId, resolveMainClusterRootName} from '../resolveClusterRootId';

describe('resolveClusterRootId', () => {
    it('returns the parent root for an element', () => {
        expect(
            resolveClusterRootId({parentClusterRootId: 'aiAgent_1', workflowNodeName: 'model_1'} as NodeDataType)
        ).toBe('aiAgent_1');
    });

    it('returns itself for a root', () => {
        expect(resolveClusterRootId({clusterRoot: true, workflowNodeName: 'aiAgent_1'} as NodeDataType)).toBe(
            'aiAgent_1'
        );
    });

    it('returns undefined for a plain task', () => {
        expect(resolveClusterRootId({workflowNodeName: 'task_1'} as NodeDataType)).toBeUndefined();
    });

    it('prefers topLevelClusterRootId over the immediate parentClusterRootId', () => {
        expect(
            resolveClusterRootId({
                parentClusterRootId: 'agentTool_1',
                topLevelClusterRootId: 'aiAgent_1',
                workflowNodeName: 'tool_2',
            } as NodeDataType)
        ).toBe('aiAgent_1');
    });

    it('falls back to parentClusterRootId when topLevelClusterRootId is absent', () => {
        expect(
            resolveClusterRootId({parentClusterRootId: 'aiAgent_1', workflowNodeName: 'tool_1'} as NodeDataType)
        ).toBe('aiAgent_1');
    });
});

describe('resolveMainClusterRootName', () => {
    const staleStoreRoot = {componentName: 'dataStream', workflowNodeName: 'dataStream_1'} as NodeDataType;

    it('takes the root from the node over a stale store root', () => {
        const tool = {
            clusterElementType: 'tools',
            parentClusterRootId: 'aiAgent_1',
            topLevelClusterRootId: 'aiAgent_1',
            workflowNodeName: 'agileCrm_1',
        } as NodeDataType;

        expect(resolveMainClusterRootName(tool, staleStoreRoot)).toBe('aiAgent_1');
    });

    it('never hands a plain task the stored root', () => {
        const task = {componentName: 'mailchimp', workflowNodeName: 'mailchimp_1'} as NodeDataType;

        expect(resolveMainClusterRootName(task, staleStoreRoot)).toBeUndefined();
    });

    it('falls back to the store for a cluster element that carries no root ids', () => {
        const orphan = {clusterElementType: 'model', workflowNodeName: 'openAi_1'} as NodeDataType;

        expect(resolveMainClusterRootName(orphan, staleStoreRoot)).toBe('dataStream_1');
    });
});
