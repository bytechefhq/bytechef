import {renderHook} from '@testing-library/react';
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
        });
    });

    describe('workflows', () => {
        it('keeps only the eligible workflows', () => {
            const {result} = renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            expect(result.current.workflows).toEqual([workflowA, workflowB]);
        });

        it('is undefined until the eligible workflow ids are known', () => {
            const {result} = renderWorkflowsForm({integrationInstanceConfigurationId: '44'});

            expect(result.current.workflows).toBeUndefined();
        });
    });

    describe('enabledWorkflowIds', () => {
        it('lists the workflows turned on in the configuration', () => {
            hoisted.useGetIntegrationInstanceConfigurationQuery.mockReturnValue({
                data: {
                    ...integrationInstanceConfiguration,
                    integrationInstanceConfigurationWorkflows: [
                        {enabled: true, workflowId: 'workflow-a'},
                        {enabled: false, workflowId: 'workflow-b'},
                    ],
                },
            });

            const {result} = renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            expect(result.current.enabledWorkflowIds).toEqual(['workflow-a']);
        });

        it('does not expose connections or inputs', () => {
            const {result} = renderWorkflowsForm({eligibleWorkflowIds, integrationInstanceConfigurationId: '44'});

            expect(Object.keys(result.current).sort()).toEqual(['enabledWorkflowIds', 'workflows']);
        });
    });
});
