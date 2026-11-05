import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    ProjectDeployment,
    ProjectDeploymentWorkflowConnection,
    Workflow,
} from '@/shared/middleware/automation/configuration';
import {act, renderHook, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useServerProjectWorkflowsForm from '../useServerProjectWorkflowsForm';

const hoisted = vi.hoisted(() => ({
    connectionsQueryResult: {data: undefined as unknown},
    projectDeploymentQueryResult: {data: undefined as unknown},
    projectVersionWorkflowsQueryResult: {data: undefined as unknown},
    useGetProjectDeploymentQuery: vi.fn(),
    useGetProjectVersionWorkflowsQuery: vi.fn(),
    useGetWorkspaceConnectionsQuery: vi.fn(),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectVersionWorkflowsQuery: hoisted.useGetProjectVersionWorkflowsQuery,
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetProjectDeploymentQuery: hoisted.useGetProjectDeploymentQuery,
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: hoisted.useGetWorkspaceConnectionsQuery,
}));

const slackWorkflow = {
    id: 'workflow-slack',
    inputs: [{name: 'channel'}],
    tasks: [
        {
            connections: [
                {
                    componentName: 'slack',
                    componentVersion: 1,
                    key: 'slack',
                    required: true,
                    workflowNodeName: 'slack_1',
                },
            ],
            name: 'slack_1',
            type: 'slack/v1/sendMessage',
        },
        {
            connections: [
                {
                    componentName: 'github',
                    componentVersion: 1,
                    key: 'github',
                    required: true,
                    workflowNodeName: 'github_1',
                },
            ],
            name: 'github_1',
            type: 'github/v1/createIssue',
        },
    ],
} as unknown as Workflow;

const emailWorkflow = {
    id: 'workflow-email',
    tasks: [],
} as unknown as Workflow;

const ineligibleWorkflow = {
    id: 'workflow-ineligible',
    tasks: [],
} as unknown as Workflow;

const PROJECT_VERSION_WORKFLOWS = [slackWorkflow, emailWorkflow, ineligibleWorkflow];

interface RenderPropsI {
    eligibleWorkflowIds?: string[];
    environmentId?: number;
    projectDeploymentId?: number;
    projectId?: number;
    projectVersion?: number;
}

const DEFAULT_PROPS: RenderPropsI = {
    eligibleWorkflowIds: ['workflow-slack', 'workflow-email'],
    environmentId: 2,
    projectId: 10,
    projectVersion: 3,
};

const renderForm = (props: RenderPropsI = DEFAULT_PROPS) =>
    renderHook((hookProps: RenderPropsI) => useServerProjectWorkflowsForm(hookProps), {initialProps: props});

describe('useServerProjectWorkflowsForm', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useWorkspaceStore.setState({currentWorkspaceId: 7});

        hoisted.connectionsQueryResult.data = [{id: 101, name: 'Slack connection'}];
        hoisted.projectDeploymentQueryResult.data = undefined;
        hoisted.projectVersionWorkflowsQueryResult.data = PROJECT_VERSION_WORKFLOWS;

        hoisted.useGetProjectVersionWorkflowsQuery.mockImplementation(() => ({
            data: hoisted.projectVersionWorkflowsQueryResult.data,
        }));
        hoisted.useGetProjectDeploymentQuery.mockImplementation(() => ({
            data: hoisted.projectDeploymentQueryResult.data,
        }));
        hoisted.useGetWorkspaceConnectionsQuery.mockImplementation(() => ({
            data: hoisted.connectionsQueryResult.data,
        }));
    });

    describe('queries', () => {
        it('enables the project version workflows query only with a project id and version', () => {
            renderForm({...DEFAULT_PROPS, projectVersion: undefined});

            expect(hoisted.useGetProjectVersionWorkflowsQuery).toHaveBeenLastCalledWith(
                10,
                undefined,
                true,
                false,
                false
            );

            renderForm();

            expect(hoisted.useGetProjectVersionWorkflowsQuery).toHaveBeenLastCalledWith(10, 3, true, true, false);
        });

        it('enables the project deployment query only with a project deployment id', () => {
            renderForm();

            expect(hoisted.useGetProjectDeploymentQuery).toHaveBeenLastCalledWith(undefined, false);

            renderForm({...DEFAULT_PROPS, projectDeploymentId: 5});

            expect(hoisted.useGetProjectDeploymentQuery).toHaveBeenLastCalledWith(5, true);
        });

        it('loads the workspace connections for the given environment', () => {
            const {result} = renderForm();

            expect(hoisted.useGetWorkspaceConnectionsQuery).toHaveBeenLastCalledWith({environmentId: 2, id: 7}, true);
            expect(result.current.connections).toEqual([{id: 101, name: 'Slack connection'}]);
        });

        it('falls back to the project deployment environment when no environment is given', () => {
            hoisted.projectDeploymentQueryResult.data = {environmentId: 4, id: 5, projectDeploymentWorkflows: []};

            renderForm({...DEFAULT_PROPS, environmentId: undefined, projectDeploymentId: 5});

            expect(hoisted.useGetWorkspaceConnectionsQuery).toHaveBeenLastCalledWith({environmentId: 4, id: 7}, true);
        });

        it('disables the connections query until an environment is known', () => {
            renderForm({...DEFAULT_PROPS, environmentId: undefined});

            expect(hoisted.useGetWorkspaceConnectionsQuery).toHaveBeenLastCalledWith(
                {environmentId: undefined, id: 7},
                false
            );
        });
    });

    describe('workflows', () => {
        it('keeps only the eligible workflows', () => {
            const {result} = renderForm();

            expect(result.current.workflows).toEqual([slackWorkflow, emailWorkflow]);
        });

        it('is undefined while the project version workflows are loading', () => {
            hoisted.projectVersionWorkflowsQueryResult.data = undefined;

            const {result} = renderForm();

            expect(result.current.workflows).toBeUndefined();
        });

        it('is undefined without eligible workflow ids', () => {
            const {result} = renderForm({...DEFAULT_PROPS, eligibleWorkflowIds: undefined});

            expect(result.current.workflows).toBeUndefined();
        });

        it('is undefined while the requested project deployment is loading', () => {
            const {result} = renderForm({...DEFAULT_PROPS, projectDeploymentId: 5});

            expect(result.current.workflows).toBeUndefined();
        });
    });

    describe('form initialization', () => {
        it('starts every eligible workflow with empty connections for each component connection', async () => {
            const {result} = renderForm();

            await waitFor(() => {
                expect(result.current.form.getValues('projectDeploymentWorkflows')).toHaveLength(2);
            });

            expect(result.current.form.getValues('projectDeploymentWorkflows')).toEqual([
                {
                    connections: [
                        {workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'},
                        {workflowConnectionKey: 'github', workflowNodeName: 'github_1'},
                    ],
                    inputs: {},
                    workflowId: 'workflow-slack',
                },
                {connections: [], inputs: {}, workflowId: 'workflow-email'},
            ]);
        });

        it('reuses the connections and inputs of an existing project deployment', async () => {
            hoisted.projectDeploymentQueryResult.data = {
                environmentId: 2,
                id: 5,
                projectDeploymentWorkflows: [
                    {
                        connections: [
                            {connectionId: 202, workflowConnectionKey: 'github', workflowNodeName: 'github_1'},
                            {connectionId: 999, workflowConnectionKey: 'stale', workflowNodeName: 'removed_1'},
                        ],
                        inputs: {channel: '#general'},
                        workflowId: 'workflow-slack',
                    },
                ],
            } as unknown as ProjectDeployment;

            const {result} = renderForm({...DEFAULT_PROPS, projectDeploymentId: 5});

            await waitFor(() => {
                expect(result.current.form.getValues('projectDeploymentWorkflows')).toHaveLength(2);
            });

            expect(result.current.form.getValues('projectDeploymentWorkflows')).toEqual([
                {
                    connections: [
                        {workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'},
                        {connectionId: 202, workflowConnectionKey: 'github', workflowNodeName: 'github_1'},
                    ],
                    inputs: {channel: '#general'},
                    workflowId: 'workflow-slack',
                },
                {connections: [], inputs: {}, workflowId: 'workflow-email'},
            ]);
        });

        it('does not overwrite user edits when re-rendered with the same workflows', async () => {
            const {rerender, result} = renderForm();

            await waitFor(() => {
                expect(result.current.form.getValues('projectDeploymentWorkflows')).toHaveLength(2);
            });

            act(() => {
                result.current.form.setValue('projectDeploymentWorkflows.0.inputs', {channel: '#edited'});
            });

            rerender({...DEFAULT_PROPS, eligibleWorkflowIds: ['workflow-slack', 'workflow-email']});

            expect(result.current.form.getValues('projectDeploymentWorkflows.0.inputs')).toEqual({
                channel: '#edited',
            });
        });

        it('re-initializes the form when the set of eligible workflows changes', async () => {
            const {rerender, result} = renderForm();

            await waitFor(() => {
                expect(result.current.form.getValues('projectDeploymentWorkflows')).toHaveLength(2);
            });

            act(() => {
                result.current.form.setValue('projectDeploymentWorkflows.0.inputs', {channel: '#edited'});
            });

            rerender({...DEFAULT_PROPS, eligibleWorkflowIds: ['workflow-slack']});

            await waitFor(() => {
                expect(result.current.form.getValues('projectDeploymentWorkflows')).toHaveLength(1);
            });

            expect(result.current.form.getValues('projectDeploymentWorkflows.0.inputs')).toEqual({});
        });
    });

    describe('getWorkflowConfigurations', () => {
        it('maps the selected workflows to configuration inputs with string connection ids', async () => {
            const {result} = renderForm();

            await waitFor(() => {
                expect(result.current.form.getValues('projectDeploymentWorkflows')).toHaveLength(2);
            });

            act(() => {
                result.current.form.setValue('projectDeploymentWorkflows.0', {
                    connections: [
                        {connectionId: 101, workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'},
                        {
                            workflowConnectionKey: 'github',
                            workflowNodeName: 'github_1',
                        } as ProjectDeploymentWorkflowConnection,
                    ],
                    inputs: {channel: '#general', unknownInput: 'ignored'},
                    workflowId: 'workflow-slack',
                });
            });

            expect(result.current.getWorkflowConfigurations(['workflow-slack', 'workflow-email'])).toEqual([
                {
                    connections: [{connectionId: '101', workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'}],
                    inputs: {channel: '#general'},
                    workflowId: 'workflow-slack',
                },
                {connections: [], inputs: {}, workflowId: 'workflow-email'},
            ]);
        });

        it('skips selected ids that are not among the eligible workflows', async () => {
            const {result} = renderForm();

            await waitFor(() => {
                expect(result.current.form.getValues('projectDeploymentWorkflows')).toHaveLength(2);
            });

            expect(result.current.getWorkflowConfigurations(['workflow-ineligible', 'missing'])).toEqual([]);
        });

        it('returns an empty configuration for a workflow without form values', () => {
            hoisted.projectVersionWorkflowsQueryResult.data = PROJECT_VERSION_WORKFLOWS;

            const {result} = renderForm();

            act(() => {
                result.current.form.reset({projectDeploymentWorkflows: []});
            });

            expect(result.current.getWorkflowConfigurations(['workflow-slack'])).toEqual([
                {connections: [], inputs: {}, workflowId: 'workflow-slack'},
            ]);
        });
    });
});
