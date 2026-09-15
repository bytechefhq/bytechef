import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowIssuesStore, {WorkflowIssueI} from '../../stores/useWorkflowIssuesStore';
import useNodeIssues from '../useNodeIssues';

const hoisted = vi.hoisted(() => ({
    workflowState: {
        workflow: {
            tasks: [
                {
                    clusterElements: {
                        model: {name: 'openAi_1', parameters: {}, type: 'openAi/v1/model'},
                        tools: [{name: 'github_1', parameters: {}, type: 'github/v1/listRepositoryIssues'}],
                    },
                    name: 'aiAgent_1',
                    parameters: {},
                    type: 'aiAgent/v1/chat',
                },
            ],
        },
    },
}));

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: typeof hoisted.workflowState) => unknown) => selector(hoisted.workflowState),
}));

const createValidatorIssue = (nodeName: string, propertyPath: string, message: string): WorkflowIssueI => ({
    kind: 'MISSING_REQUIRED',
    message,
    nodeName,
    propertyPath,
    severity: 'ERROR',
    source: 'VALIDATOR',
});

describe('useNodeIssues', () => {
    beforeEach(() => {
        useWorkflowIssuesStore.getState().reset();
    });

    it('does not count cluster element issues on a cluster root whose own properties are valid', () => {
        useWorkflowIssuesStore
            .getState()
            .setValidatorIssues([
                createValidatorIssue('aiAgent_1', 'openAi_1.model', 'Missing required property: openAi_1.model'),
                createValidatorIssue('aiAgent_1', 'github_1.owner', 'Missing required property: github_1.owner'),
            ]);

        const {result} = renderHook(() => useNodeIssues('aiAgent_1'));

        expect(result.current).toEqual({count: 0, severity: undefined, title: undefined});
    });

    it('does not count cluster element issues on a cluster root loaded from server task DTOs', () => {
        const previousTasks = hoisted.workflowState.workflow.tasks;

        hoisted.workflowState.workflow.tasks = [
            {
                clusterElements: {
                    tools: [
                        {componentName: 'github', workflowNodeName: 'github_1'},
                        {componentName: 'github', workflowNodeName: 'github_2'},
                    ],
                },
                name: 'aiAgent_1',
                parameters: {},
                type: 'aiAgent/v1/chat',
            },
        ] as unknown as typeof previousTasks;

        useWorkflowIssuesStore
            .getState()
            .setValidatorIssues([
                createValidatorIssue('aiAgent_1', 'github_1.owner', 'Missing required property: github_1.owner'),
                createValidatorIssue('aiAgent_1', 'github_2.owner', 'Missing required property: github_2.owner'),
                createValidatorIssue('aiAgent_1', 'github_2.issue', 'Missing required property: github_2.issue'),
            ]);

        const {result: rootResult} = renderHook(() => useNodeIssues('aiAgent_1'));
        const {result: githubResult} = renderHook(() => useNodeIssues('github_2'));

        hoisted.workflowState.workflow.tasks = previousTasks;

        expect(rootResult.current.count).toBe(0);
        expect(githubResult.current.count).toBe(2);
    });

    it('counts cluster element issues on a collapsed cluster root when including cluster element issues', () => {
        useWorkflowIssuesStore
            .getState()
            .setValidatorIssues([
                createValidatorIssue('aiAgent_1', 'openAi_1.model', 'Missing required property: openAi_1.model'),
                createValidatorIssue('aiAgent_1', 'github_1.owner', 'Missing required property: github_1.owner'),
            ]);

        const {result} = renderHook(() => useNodeIssues('aiAgent_1', true));

        expect(result.current).toEqual({
            count: 2,
            severity: 'ERROR',
            title: 'Missing required property: openAi_1.model\nMissing required property: github_1.owner',
        });
    });

    it('counts issues on the cluster root own properties', () => {
        useWorkflowIssuesStore
            .getState()
            .setValidatorIssues([
                createValidatorIssue('aiAgent_1', 'prompt', 'Missing required property: prompt'),
                createValidatorIssue('aiAgent_1', 'openAi_1.model', 'Missing required property: openAi_1.model'),
            ]);

        const {result} = renderHook(() => useNodeIssues('aiAgent_1'));

        expect(result.current).toEqual({
            count: 1,
            severity: 'ERROR',
            title: 'Missing required property: prompt',
        });
    });

    it('counts cluster element issues on the cluster element they belong to', () => {
        useWorkflowIssuesStore
            .getState()
            .setValidatorIssues([
                createValidatorIssue('aiAgent_1', 'openAi_1.model', 'Missing required property: openAi_1.model'),
                createValidatorIssue('aiAgent_1', 'github_1.owner', 'Missing required property: github_1.owner'),
            ]);

        const {result: openAiResult} = renderHook(() => useNodeIssues('openAi_1'));
        const {result: githubResult} = renderHook(() => useNodeIssues('github_1'));

        expect(openAiResult.current.count).toBe(1);
        expect(openAiResult.current.title).toBe('Missing required property: openAi_1.model');
        expect(githubResult.current.count).toBe(1);
        expect(githubResult.current.title).toBe('Missing required property: github_1.owner');
    });

    it('counts cluster element missing connections on both the cluster element and its cluster root', () => {
        useWorkflowIssuesStore.getState().setValidatorIssues([
            {
                kind: 'MISSING_CONNECTION',
                message: 'Missing required connection: GitHub',
                nodeName: 'github_1',
                severity: 'ERROR',
                source: 'VALIDATOR',
            },
        ]);

        const {result: githubResult} = renderHook(() => useNodeIssues('github_1'));
        const {result: expandedRootResult} = renderHook(() => useNodeIssues('aiAgent_1'));
        const {result: collapsedRootResult} = renderHook(() => useNodeIssues('aiAgent_1', true));

        expect(githubResult.current.count).toBe(1);
        expect(expandedRootResult.current).toEqual({
            count: 1,
            severity: 'ERROR',
            title: 'Missing required connection: GitHub',
        });
        expect(collapsedRootResult.current.count).toBe(1);
    });

    it('counts a broken data pill reference on the task that holds it', () => {
        useWorkflowIssuesStore.getState().setSweepIssues([
            {
                kind: 'BROKEN_REFERENCE',
                message: '"approval_1" is missing from the workflow (referenced as approval_1.comment)',
                nodeName: 'logger_1',
                propertyPath: 'approval_1.comment',
                severity: 'ERROR',
                source: 'SWEEP',
            },
        ]);

        const {result: loggerResult} = renderHook(() => useNodeIssues('logger_1'));
        const {result: rootResult} = renderHook(() => useNodeIssues('aiAgent_1', true));

        expect(loggerResult.current.count).toBe(1);
        expect(rootResult.current.count).toBe(0);
    });
});
