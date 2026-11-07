import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getClusterRootTask} from './getClusterRootTask';

const AI_AGENT_TASK = {clusterElements: {tools: []}, name: 'aiAgent_1', type: 'aiAgent/v1/chat'} as WorkflowTask;

const LOOP_TASK = {
    name: 'loop_1',
    parameters: {iteratee: [{clusterElements: {}, name: 'aiAgent_2', type: 'aiAgent/v1/chat'}]},
    type: 'loop/v1',
} as WorkflowTask;

const VOICE_SESSION_TRIGGER = {
    clusterElements: {
        tools: [],
        voiceAgent: {name: 'voiceAgent_1', parameters: {}, type: 'deepgram/v1/voiceAgent'},
    },
    name: 'trigger_1',
    type: 'browser/v1/voiceSession',
} as WorkflowTrigger;

describe('getClusterRootTask', () => {
    it('finds a cluster root among the top-level tasks', () => {
        expect(
            getClusterRootTask({
                tasks: [AI_AGENT_TASK],
                triggers: [VOICE_SESSION_TRIGGER],
                workflowNodeName: 'aiAgent_1',
            })
        ).toBe(AI_AGENT_TASK);
    });

    it('finds a cluster root nested inside a task dispatcher', () => {
        expect(getClusterRootTask({tasks: [LOOP_TASK], triggers: [], workflowNodeName: 'aiAgent_2'})?.name).toBe(
            'aiAgent_2'
        );
    });

    it('finds a cluster root that is a trigger', () => {
        const clusterRoot = getClusterRootTask({
            tasks: [AI_AGENT_TASK],
            triggers: [VOICE_SESSION_TRIGGER],
            workflowNodeName: 'trigger_1',
        });

        expect(clusterRoot?.type).toBe('browser/v1/voiceSession');
        expect(clusterRoot?.clusterElements?.voiceAgent.type).toBe('deepgram/v1/voiceAgent');
    });

    it('tolerates a definition with no tasks or no triggers key', () => {
        expect(getClusterRootTask({triggers: [VOICE_SESSION_TRIGGER], workflowNodeName: 'trigger_1'})?.name).toBe(
            'trigger_1'
        );
        expect(getClusterRootTask({tasks: [AI_AGENT_TASK], workflowNodeName: 'trigger_1'})).toBeUndefined();
    });
});
