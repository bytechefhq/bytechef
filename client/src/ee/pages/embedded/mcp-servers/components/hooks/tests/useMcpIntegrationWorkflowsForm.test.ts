import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpIntegrationWorkflowsForm from '../useMcpIntegrationWorkflowsForm';

const hoisted = vi.hoisted(() => ({
    useGetIntegrationInstanceConfigurationQuery: vi.fn(),
    useGetIntegrationVersionWorkflowsQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationQuery: hoisted.useGetIntegrationInstanceConfigurationQuery,
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: hoisted.useGetIntegrationVersionWorkflowsQuery,
}));

const slackConnection = {
    componentName: 'slack',
    componentVersion: 1,
    key: 'slack',
    required: true,
    workflowNodeName: 'slack_1',
};

const hubspotConnection = {
    componentName: 'hubspot',
    componentVersion: 1,
    key: 'hubspot',
    required: true,
    workflowNodeName: 'hubspot_1',
};

const githubTriggerConnection = {
    componentName: 'github',
    componentVersion: 1,
    key: 'github',
    required: true,
    workflowNodeName: 'trigger_1',
};

const workflowA = {
    id: 'workflow-a',
    label: 'Notify',
    tasks: [{connections: [slackConnection, hubspotConnection]}, {}],
    triggers: [{connections: [githubTriggerConnection]}],
};

const workflowB = {id: 'workflow-b', label: 'Sync'};

const workflowC = {id: 'workflow-c', label: 'Ineligible', tasks: [{connections: [slackConnection]}]};

const integrationVersionWorkflows = [workflowA, workflowB, workflowC];

const integrationInstanceConfiguration = {
    id: 44,
    integration: {componentName: 'hubspot'},
    integrationId: 7,
    integrationInstanceConfigurationWorkflows: [
        {
            connections: [{connectionId: 501, workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'}],
            inputs: {channel: '#general'},
            workflowId: 'workflow-a',
        },
    ],
    integrationVersion: 3,
};

const eligibleWorkflowIds = ['workflow-a', 'workflow-b'];

interface HookPropsI {
    eligibleWorkflowIds?: string[];
    integrationInstanceConfigurationId?: string;
}

const renderWorkflowsForm = (initialProps: HookPropsI) =>
    renderHook((props: HookPropsI) => useMcpIntegrationWorkflowsForm(props), {initialProps});

describe('useMcpIntegrationWorkflowsForm', () => {
    beforeEach(() => {
        hoisted.useGetIntegrationInstanceConfigurationQuery.mockReset();
        hoisted.useGetIntegrationInstanceConfigurationQuery.mockReturnValue({data: integrationInstanceConfiguration});
        hoisted.useGetIntegrationVersionWorkflowsQuery.mockReset();
        hoisted.useGetIntegrationVersionWorkflowsQuery.mockReturnValue({data: integrationVersionWorkflows});
    });

    describe('queries', () => {
        it('loads the configuration and the workflows of its integration version', () => {
            renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            expect(hoisted.useGetIntegrationInstanceConfigurationQuery).toHaveBeenLastCalledWith(44, true);
            expect(hoisted.useGetIntegrationVersionWorkflowsQuery).toHaveBeenLastCalledWith(7, 3, true, true);
        });

        it('does not load anything without a configuration id', () => {
            hoisted.useGetIntegrationInstanceConfigurationQuery.mockReturnValue({data: undefined});
            hoisted.useGetIntegrationVersionWorkflowsQuery.mockReturnValue({data: undefined});

            const {result} = renderWorkflowsForm({eligibleWorkflowIds});

            expect(hoisted.useGetIntegrationInstanceConfigurationQuery.mock.lastCall?.[1]).toBe(false);
            expect(hoisted.useGetIntegrationVersionWorkflowsQuery).toHaveBeenLastCalledWith(0, 0, true, false);
            expect(result.current.workflows).toBeUndefined();
            expect(result.current.componentName).toBeUndefined();
        });
    });

    describe('workflows', () => {
        it('keeps only the eligible workflows', () => {
            const {result} = renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            expect(result.current.workflows).toEqual([workflowA, workflowB]);
            expect(result.current.componentName).toBe('hubspot');
        });

        it('is undefined until the eligible workflow ids are known', () => {
            const {result} = renderWorkflowsForm({integrationInstanceConfigurationId: '44'});

            expect(result.current.workflows).toBeUndefined();
            expect(result.current.form.getValues('integrationInstanceConfigurationWorkflows')).toEqual([]);
        });
    });

    describe('form initialization', () => {
        it('initializes each workflow with its configurable connections and saved values', () => {
            const {result} = renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            expect(result.current.form.getValues('integrationInstanceConfigurationWorkflows')).toEqual([
                {
                    connections: [
                        {connectionId: 501, workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'},
                        {workflowConnectionKey: 'github', workflowNodeName: 'trigger_1'},
                    ],
                    inputs: {channel: '#general'},
                    workflowId: 'workflow-a',
                },
                {connections: [], inputs: {}, workflowId: 'workflow-b'},
            ]);
        });

        it('keeps user edits when re-rendered with the same workflows', () => {
            const {rerender, result} = renderWorkflowsForm({
                eligibleWorkflowIds,
                integrationInstanceConfigurationId: '44',
            });

            act(() => {
                result.current.form.setValue('integrationInstanceConfigurationWorkflows.1.inputs', {limit: 5});
            });

            rerender({eligibleWorkflowIds: [...eligibleWorkflowIds], integrationInstanceConfigurationId: '44'});

            expect(result.current.form.getValues('integrationInstanceConfigurationWorkflows.1.inputs')).toEqual({
                limit: 5,
            });
        });

        it('reinitializes when the eligible workflows change', () => {
            const {rerender, result} = renderWorkflowsForm({
                eligibleWorkflowIds,
                integrationInstanceConfigurationId: '44',
            });

            rerender({eligibleWorkflowIds: ['workflow-b'], integrationInstanceConfigurationId: '44'});

            expect(result.current.form.getValues('integrationInstanceConfigurationWorkflows')).toEqual([
                {connections: [], inputs: {}, workflowId: 'workflow-b'},
            ]);
        });
    });

    describe('getWorkflowConfigurations', () => {
        it('builds the configuration of each selected workflow from the form', () => {
            const {result} = renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            act(() => {
                result.current.form.setValue(
                    'integrationInstanceConfigurationWorkflows.0.connections.1.connectionId',
                    777
                );
            });

            expect(result.current.getWorkflowConfigurations(['workflow-b', 'workflow-a'])).toEqual([
                {connections: [], inputs: {}, workflowId: 'workflow-b'},
                {
                    connections: [
                        {connectionId: '501', workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'},
                        {connectionId: '777', workflowConnectionKey: 'github', workflowNodeName: 'trigger_1'},
                    ],
                    inputs: {channel: '#general'},
                    workflowId: 'workflow-a',
                },
            ]);
        });

        it('skips connections without a numeric connection id', () => {
            const {result} = renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            act(() => {
                result.current.form.setValue(
                    'integrationInstanceConfigurationWorkflows.0.connections.0.connectionId',
                    'abc' as unknown as number
                );
            });

            expect(result.current.getWorkflowConfigurations(['workflow-a'])).toEqual([
                {connections: [], inputs: {channel: '#general'}, workflowId: 'workflow-a'},
            ]);
        });

        it('skips workflows that are not eligible', () => {
            const {result} = renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            expect(result.current.getWorkflowConfigurations(['workflow-c', 'unknown'])).toEqual([]);
        });

        it('returns nothing while the workflows are loading', () => {
            hoisted.useGetIntegrationVersionWorkflowsQuery.mockReturnValue({data: undefined});

            const {result} = renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            expect(result.current.getWorkflowConfigurations(['workflow-a'])).toEqual([]);
        });
    });
});
