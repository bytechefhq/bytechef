import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowIssuesStore, {WorkflowIssueI} from '../../stores/useWorkflowIssuesStore';
import useWorkflowIssuesValidation from '../useWorkflowIssuesValidation';

const hoisted = vi.hoisted(() => ({
    queryResult: {data: undefined as unknown},
    queryVariables: [] as Array<unknown>,
    workflowState: {workflow: {definition: '{"tasks":[]}', id: 'wf-1'}},
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useValidateWorkflowQuery: (variables: unknown) => {
        hoisted.queryVariables.push(variables);

        return hoisted.queryResult;
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: typeof hoisted.workflowState) => unknown) => selector(hoisted.workflowState),
}));

const VALIDATOR_ISSUE: WorkflowIssueI = {
    kind: 'MISSING_RESOURCE',
    message: 'gone',
    nodeName: 'dataTable_1',
    propertyPath: 'table',
    severity: 'ERROR',
    source: 'VALIDATOR',
};

describe('useWorkflowIssuesValidation', () => {
    beforeEach(() => {
        hoisted.queryVariables = [];
        hoisted.queryResult = {data: undefined};
        hoisted.workflowState = {workflow: {definition: '{"tasks":[]}', id: 'wf-1'}};
        useWorkflowIssuesStore.getState().reset();
    });

    it('queries with the definition and the selected environment', () => {
        renderHook(() => useWorkflowIssuesValidation());

        expect(hoisted.queryVariables[0]).toEqual({
            environmentId: 2,
            workflowDefinition: '{"tasks":[]}',
            workflowId: 'wf-1',
        });
    });

    it('stores node issues from the response as validator issues', () => {
        hoisted.queryResult = {
            data: {
                validateWorkflow: {
                    errors: [],
                    nodeIssues: [
                        {
                            kind: 'MISSING_RESOURCE',
                            message: 'gone',
                            nodeName: 'dataTable_1',
                            propertyPath: 'table',
                            severity: 'ERROR',
                        },
                    ],
                    warnings: [],
                },
            },
        };

        renderHook(() => useWorkflowIssuesValidation());

        expect(useWorkflowIssuesStore.getState().validatorIssues).toEqual([
            {
                kind: 'MISSING_RESOURCE',
                message: 'gone',
                nodeName: 'dataTable_1',
                propertyPath: 'table',
                severity: 'ERROR',
                source: 'VALIDATOR',
            },
        ]);
    });

    it('clears live issues when the definition changes', () => {
        const {rerender} = renderHook(() => useWorkflowIssuesValidation());

        useWorkflowIssuesStore.getState().recordLookupFailure('dataTable_1', 'table', 'stale');

        hoisted.workflowState = {workflow: {definition: '{"tasks":[{}]}', id: 'wf-1'}};

        rerender();

        expect(useWorkflowIssuesStore.getState().liveIssues).toEqual({});
    });

    it('keeps validator issues while a changed definition revalidates', () => {
        const {rerender} = renderHook(() => useWorkflowIssuesValidation());

        useWorkflowIssuesStore.getState().setValidatorIssues([VALIDATOR_ISSUE]);

        hoisted.workflowState = {workflow: {definition: '{"tasks":[{}]}', id: 'wf-1'}};

        rerender();

        expect(useWorkflowIssuesStore.getState().validatorIssues).toEqual([VALIDATOR_ISSUE]);
    });

    it('clears validator issues when another workflow opens', () => {
        const {rerender} = renderHook(() => useWorkflowIssuesValidation());

        useWorkflowIssuesStore.getState().setValidatorIssues([VALIDATOR_ISSUE]);

        hoisted.workflowState = {workflow: {definition: '{"tasks":[]}', id: 'wf-2'}};

        rerender();

        expect(useWorkflowIssuesStore.getState().validatorIssues).toEqual([]);
    });
});
