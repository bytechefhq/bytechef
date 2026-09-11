import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import findClusterElementRootTaskName from '../findClusterElementRootTaskName';

const tasks = [
    {name: 'logger_1', type: 'logger/v1/info'},
    {
        clusterElements: {
            model: {clusterElementName: 'model', type: 'openAi/v1/model', workflowNodeName: 'openAi_1'},
            tools: [
                {
                    clusterElements: {model: {name: 'openAi_2', type: 'openAi/v1/model'}},
                    type: 'aiAgent/v1/chat',
                    workflowNodeName: 'aiAgent_2',
                },
                {name: 'httpClient_1', type: 'httpClient/v1/get'},
            ],
        },
        name: 'aiAgent_1',
        type: 'aiAgent/v1/chat',
    },
] as Array<WorkflowTask>;

describe('findClusterElementRootTaskName', () => {
    it('matches the server-flattened shape that carries workflowNodeName', () => {
        expect(findClusterElementRootTaskName(tasks, 'openAi_1')).toBe('aiAgent_1');
    });

    it('matches the raw definition shape that carries name, inside arrays too', () => {
        expect(findClusterElementRootTaskName(tasks, 'httpClient_1')).toBe('aiAgent_1');
    });

    it('matches elements nested inside a nested cluster root', () => {
        expect(findClusterElementRootTaskName(tasks, 'openAi_2')).toBe('aiAgent_1');
    });

    it('returns undefined for a regular node or an unknown name', () => {
        expect(findClusterElementRootTaskName(tasks, 'logger_1')).toBeUndefined();
        expect(findClusterElementRootTaskName(undefined, 'openAi_1')).toBeUndefined();
    });
});
