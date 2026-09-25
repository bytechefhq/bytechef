import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getWorkflowIssueOwnerName, {
    getClusterElementRootNames,
    getWorkflowIssueOwnerPropertyPath,
} from './getWorkflowIssueOwnerName';

const AI_AGENT_TASK = {
    clusterElements: {
        model: {name: 'openAi_1', parameters: {}, type: 'openAi/v1/model'},
        rag: {
            clusterElements: {
                vectorStore: {name: 'pgVector_1', parameters: {}, type: 'pgVector/v1/vectorStore'},
            },
            name: 'rag_1',
            parameters: {},
            type: 'rag/v1/rag',
        },
        tools: [
            {name: 'github_1', parameters: {}, type: 'github/v1/listRepositoryIssues'},
            {name: 'github_2', parameters: {}, type: 'github/v1/getIssue'},
        ],
    },
    name: 'aiAgent_1',
    parameters: {},
    type: 'aiAgent/v1/chat',
} as unknown as WorkflowTask;

describe('getClusterElementRootNames', () => {
    it('maps single, array and nested cluster elements to their main cluster root', () => {
        expect(getClusterElementRootNames([AI_AGENT_TASK])).toEqual(
            new Map([
                ['github_1', 'aiAgent_1'],
                ['github_2', 'aiAgent_1'],
                ['openAi_1', 'aiAgent_1'],
                ['pgVector_1', 'aiAgent_1'],
                ['rag_1', 'aiAgent_1'],
            ])
        );
    });

    it('maps cluster elements of cluster roots nested in task dispatchers', () => {
        const conditionTask = {
            name: 'condition_1',
            parameters: {caseFalse: [], caseTrue: [AI_AGENT_TASK]},
            type: 'condition/v1',
        } as unknown as WorkflowTask;

        expect(getClusterElementRootNames([conditionTask]).get('openAi_1')).toBe('aiAgent_1');
    });

    it('maps cluster elements of server task DTOs that use workflowNodeName', () => {
        const serverAiAgentTask = {
            clusterElements: {
                model: {componentName: 'openAi', workflowNodeName: 'openAi_1'},
                tools: [
                    {componentName: 'github', workflowNodeName: 'github_1'},
                    {componentName: 'github', workflowNodeName: 'github_2'},
                ],
            },
            name: 'aiAgent_1',
            parameters: {},
            type: 'aiAgent/v1/chat',
        } as unknown as WorkflowTask;

        expect(getClusterElementRootNames([serverAiAgentTask])).toEqual(
            new Map([
                ['github_1', 'aiAgent_1'],
                ['github_2', 'aiAgent_1'],
                ['openAi_1', 'aiAgent_1'],
            ])
        );
    });

    it('returns an empty map when there are no tasks', () => {
        expect(getClusterElementRootNames(undefined)).toEqual(new Map());
    });
});

describe('getWorkflowIssueOwnerName', () => {
    const clusterElementRootNames = getClusterElementRootNames([AI_AGENT_TASK]);

    it('assigns an issue on a cluster root property to the cluster root', () => {
        expect(
            getWorkflowIssueOwnerName({nodeName: 'aiAgent_1', propertyPath: 'prompt'}, clusterElementRootNames)
        ).toBe('aiAgent_1');
    });

    it('assigns an issue without a property path to the reported node', () => {
        expect(getWorkflowIssueOwnerName({nodeName: 'aiAgent_1'}, clusterElementRootNames)).toBe('aiAgent_1');
        expect(getWorkflowIssueOwnerName({nodeName: 'github_1'}, clusterElementRootNames)).toBe('github_1');
    });

    it('assigns a cluster element issue reported under the cluster root to the cluster element', () => {
        expect(
            getWorkflowIssueOwnerName({nodeName: 'aiAgent_1', propertyPath: 'openAi_1.model'}, clusterElementRootNames)
        ).toBe('openAi_1');
        expect(
            getWorkflowIssueOwnerName({nodeName: 'aiAgent_1', propertyPath: 'github_2.issue'}, clusterElementRootNames)
        ).toBe('github_2');
    });

    it('assigns a nested cluster element issue to the deepest cluster element', () => {
        expect(
            getWorkflowIssueOwnerName(
                {nodeName: 'aiAgent_1', propertyPath: 'rag_1.pgVector_1.table'},
                clusterElementRootNames
            )
        ).toBe('pgVector_1');
        expect(
            getWorkflowIssueOwnerName({nodeName: 'aiAgent_1', propertyPath: 'rag_1.query'}, clusterElementRootNames)
        ).toBe('rag_1');
    });

    it('does not treat a cluster element name later in the path as the owner', () => {
        expect(
            getWorkflowIssueOwnerName(
                {nodeName: 'httpClient_1', propertyPath: 'body.openAi_1.value'},
                clusterElementRootNames
            )
        ).toBe('httpClient_1');
    });
});

describe('getWorkflowIssueOwnerPropertyPath', () => {
    const clusterElementRootNames = getClusterElementRootNames([AI_AGENT_TASK]);

    it('drops the cluster element names that lead to the owning element', () => {
        expect(getWorkflowIssueOwnerPropertyPath({propertyPath: 'openAi_1.model'}, clusterElementRootNames)).toBe(
            'model'
        );
        expect(
            getWorkflowIssueOwnerPropertyPath({propertyPath: 'rag_1.pgVector_1.index'}, clusterElementRootNames)
        ).toBe('index');
    });

    it('keeps a path that does not start with a cluster element name', () => {
        expect(getWorkflowIssueOwnerPropertyPath({propertyPath: 'prompt'}, clusterElementRootNames)).toBe('prompt');
        expect(
            getWorkflowIssueOwnerPropertyPath({propertyPath: 'firecrawl_5.data.json'}, clusterElementRootNames)
        ).toBe('firecrawl_5.data.json');
    });

    it('returns nothing when there is no path or the path names only cluster elements', () => {
        expect(getWorkflowIssueOwnerPropertyPath({}, clusterElementRootNames)).toBeUndefined();
        expect(getWorkflowIssueOwnerPropertyPath({propertyPath: 'openAi_1'}, clusterElementRootNames)).toBeUndefined();
    });
});
