import {NodeDataType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {resolveClusterRootId} from '../resolveClusterRootId';

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
