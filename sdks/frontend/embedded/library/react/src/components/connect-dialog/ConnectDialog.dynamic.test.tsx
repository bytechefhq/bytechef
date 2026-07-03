import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import ConnectDialog from './ConnectDialog';
import {MergedWorkflowType} from './types';

const baseProps = {
    apiFetch: vi.fn().mockResolvedValue([]),
    closeDialog: vi.fn(),
    handleClick: vi.fn(),
    handleMcpWorkflowGroupInputChange: vi.fn(),
    handleWorkflowToggle: vi.fn(),
    handleWorkflowInputChange: vi.fn(),
    handleWorkflowGroupInputChange: vi.fn(),
    integration: {id: 1, name: 'Test Integration'},
    integrationInstanceId: 1,
    isOpen: true,
    loading: false,
    mergedMcpTools: [],
    mergedMcpWorkflows: [],
    workflowsView: true,
};

describe('ConnectDialog dynamic inputs', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    it('fetches and renders options for a single-property dynamic group member', async () => {
        const workflowUuid = 'wf-1';
        const apiFetch = vi.fn().mockResolvedValue([
            {label: 'General', value: 'C1'},
            {label: 'Random', value: 'C2'},
        ]);
        const mergedWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'slack',
                            componentVersion: 1,
                            group: {
                                name: 'channel',
                                properties: [{dynamicOptions: true, label: 'Channel', name: 'channelId'}],
                            },
                            groupName: 'channel',
                        },
                        label: 'Channel',
                        name: 'channel',
                        type: 'object',
                    },
                ],
                label: 'Workflow 1',
                workflowUuid,
            },
        ];

        render(<ConnectDialog {...baseProps} apiFetch={apiFetch} mergedWorkflows={mergedWorkflows} />);

        expect(apiFetch).toHaveBeenCalledWith('/api/embedded/v1/integration-instances/1/component-input-options', {
            body: {
                componentName: 'slack',
                componentVersion: 1,
                groupName: 'channel',
                lookupDependsOnValues: {},
                propertyName: 'channelId',
            },
            method: 'POST',
        });

        expect(await screen.findByText('General')).toBeTruthy();
        expect(await screen.findByText('Random')).toBeTruthy();
    });

    it('renders the member fields of a property group and reports member changes', () => {
        const workflowUuid = 'wf-2';
        const mergedWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'googleSheets',
                            componentVersion: 1,
                            group: {
                                label: 'Spreadsheet location',
                                name: 'location',
                                properties: [
                                    {label: 'Spreadsheet', name: 'spreadsheetId'},
                                    {label: 'Sheet', name: 'sheetName'},
                                ],
                            },
                            groupName: 'location',
                        },
                        label: 'Location',
                        name: 'location',
                        type: 'object',
                    },
                ],
                label: 'Workflow Group',
                workflowUuid,
            },
        ];

        render(<ConnectDialog {...baseProps} mergedWorkflows={mergedWorkflows} />);

        expect(screen.getByText('Spreadsheet location')).toBeTruthy();
        expect(screen.getByLabelText('Spreadsheet')).toBeTruthy();
        expect(screen.getByLabelText('Sheet')).toBeTruthy();

        fireEvent.change(screen.getByLabelText('Spreadsheet'), {target: {value: 'spreadsheet-1'}});

        expect(baseProps.handleWorkflowGroupInputChange).toHaveBeenCalledWith(
            workflowUuid,
            'location',
            'spreadsheetId',
            'spreadsheet-1'
        );
    });

    it('disables a dependent member select and does not fetch until the dependency is present', () => {
        const apiFetch = vi.fn().mockResolvedValue([]);
        const mergedWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'slack',
                            componentVersion: 1,
                            group: {
                                name: 'channel',
                                properties: [
                                    {label: 'Workspace', name: 'workspace'},
                                    {
                                        dynamicOptions: true,
                                        label: 'Channel',
                                        name: 'channelId',
                                        optionsLookupDependsOn: ['workspace'],
                                    },
                                ],
                            },
                            groupName: 'channel',
                        },
                        label: 'Channel',
                        name: 'channel',
                        type: 'object',
                    },
                ],
                label: 'Dependent Workflow',
                workflowUuid: 'wf-3',
            },
        ];

        render(<ConnectDialog {...baseProps} apiFetch={apiFetch} mergedWorkflows={mergedWorkflows} />);

        const select = screen.getByLabelText('Channel') as HTMLSelectElement;

        expect(select.disabled).toBe(true);
        expect(screen.getByText('Select dependencies first')).toBeTruthy();
        expect(apiFetch).not.toHaveBeenCalled();
    });

    it('fetches options once a previously unsatisfied dependency becomes available', () => {
        const workflowUuid = 'wf-3';
        const apiFetch = vi.fn().mockResolvedValue([]);
        const buildWorkflows = (workspaceValue: string): MergedWorkflowType[] => [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'slack',
                            componentVersion: 1,
                            group: {
                                name: 'channel',
                                properties: [
                                    {label: 'Workspace', name: 'workspace'},
                                    {
                                        dynamicOptions: true,
                                        label: 'Channel',
                                        name: 'channelId',
                                        optionsLookupDependsOn: ['workspace'],
                                    },
                                ],
                            },
                            groupName: 'channel',
                        },
                        label: 'Channel',
                        name: 'channel',
                        type: 'object',
                        value: {workspace: workspaceValue},
                    },
                ],
                label: 'Dependent Workflow',
                workflowUuid,
            },
        ];

        const {rerender} = render(
            <ConnectDialog {...baseProps} apiFetch={apiFetch} mergedWorkflows={buildWorkflows('')} />
        );

        expect(apiFetch).not.toHaveBeenCalled();

        rerender(<ConnectDialog {...baseProps} apiFetch={apiFetch} mergedWorkflows={buildWorkflows('W1')} />);

        expect(apiFetch).toHaveBeenCalledWith('/api/embedded/v1/integration-instances/1/component-input-options', {
            body: {
                componentName: 'slack',
                componentVersion: 1,
                groupName: 'channel',
                lookupDependsOnValues: {workspace: 'W1'},
                propertyName: 'channelId',
            },
            method: 'POST',
        });
    });

    it('falls back to a plain text input when a component reference has no resolved group', () => {
        const mergedWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {componentName: 'slack', componentVersion: 1, groupName: 'missing'},
                        label: 'Dangling',
                        name: 'dangling',
                        type: 'string',
                    },
                ],
                label: 'Dangling Workflow',
                workflowUuid: 'wf-4',
            },
        ];

        render(<ConnectDialog {...baseProps} mergedWorkflows={mergedWorkflows} />);

        const input = screen.getByLabelText('Dangling') as HTMLInputElement;

        expect(input.tagName).toBe('INPUT');
    });

    it('fetches MCP-workflow group-member options from the same workflows options endpoint', async () => {
        const workflowUuid = 'mcp-wf-1';
        const apiFetch = vi.fn().mockResolvedValue([{label: 'General', value: 'C1'}]);
        const mergedMcpWorkflows: MergedWorkflowType[] = [
            {
                enabled: true,
                inputs: [
                    {
                        componentReference: {
                            componentName: 'slack',
                            componentVersion: 1,
                            group: {
                                name: 'channel',
                                properties: [{dynamicOptions: true, label: 'Channel', name: 'channelId'}],
                            },
                            groupName: 'channel',
                        },
                        label: 'Channel',
                        name: 'channel',
                        type: 'object',
                    },
                ],
                label: 'MCP Workflow 1',
                workflowUuid,
            },
        ];

        render(
            <ConnectDialog
                {...baseProps}
                apiFetch={apiFetch}
                mergedWorkflows={[]}
                mergedMcpWorkflows={mergedMcpWorkflows}
            />
        );

        expect(apiFetch).toHaveBeenCalledWith('/api/embedded/v1/integration-instances/1/component-input-options', {
            body: {
                componentName: 'slack',
                componentVersion: 1,
                groupName: 'channel',
                lookupDependsOnValues: {},
                propertyName: 'channelId',
            },
            method: 'POST',
        });

        expect(await screen.findByText('General')).toBeTruthy();

        fireEvent.change(screen.getByLabelText('Channel'), {target: {value: 'C1'}});

        await waitFor(() =>
            expect(baseProps.handleMcpWorkflowGroupInputChange).toHaveBeenCalledWith(
                workflowUuid,
                'channel',
                'channelId',
                'C1'
            )
        );
    });
});

describe('optionsCacheKey', () => {
    it('produces distinct keys for distinct dependency values and a stable key for equal values', async () => {
        const {optionsCacheKey} = await import('./utils');

        const first = optionsCacheKey('slack', 1, 'channel', 'channelId', {teamId: 'T1'});
        const second = optionsCacheKey('slack', 1, 'channel', 'channelId', {teamId: 'T2'});
        const repeated = optionsCacheKey('slack', 1, 'channel', 'channelId', {teamId: 'T1'});

        expect(first).not.toBe(second);
        expect(first).toBe(repeated);
    });

    it('does not collide for distinct inputs that share a property name', async () => {
        const {optionsCacheKey} = await import('./utils');

        const topLevel = optionsCacheKey('slack', 1, 'channel', 'channelId', {});
        const groupMember = optionsCacheKey('slack', 1, 'location', 'channelId', {});

        expect(topLevel).not.toBe(groupMember);
    });
});
