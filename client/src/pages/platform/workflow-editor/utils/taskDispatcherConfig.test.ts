import {describe, expect, it} from 'vitest';

import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

describe('TASK_DISPATCHER_CONFIG.callAiAgent', () => {
    it('should read callAiAgentId off the context', () => {
        expect(
            TASK_DISPATCHER_CONFIG.callAiAgent.getDispatcherId({callAiAgentId: 'agent_1', taskDispatcherId: 'agent_1'})
        ).toBe('agent_1');
    });

    it('should have no subtasks', () => {
        expect(TASK_DISPATCHER_CONFIG.callAiAgent.getSubtasks()).toEqual([]);
    });

    it('should parse the dispatcher id from a placeholder', () => {
        expect(TASK_DISPATCHER_CONFIG.callAiAgent.extractContextFromPlaceholder('agent_1-0')).toEqual({
            index: 0,
            taskDispatcherId: 'agent_1',
        });
    });
});
