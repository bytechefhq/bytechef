import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {ComponentDefinitionBasic, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

type WorkflowDataStoreWorkflowType = ReturnType<typeof useWorkflowDataStore.getState>['workflow'];

const {workflowTestConfigurationRef} = vi.hoisted(() => ({
    workflowTestConfigurationRef: {current: undefined as WorkflowTestConfiguration | undefined},
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    useGetWorkflowTestConfigurationQuery: () => ({data: workflowTestConfigurationRef.current}),
}));

const componentDefinitions = [{name: 'googleMail', title: 'Gmail'}] as ComponentDefinitionBasic[];

const workflow = {
    id: 'workflow-1',
    inputs: [
        {label: 'Sender', name: 'sender', required: true},
        {name: 'subject', required: true},
        {label: 'Folder', name: 'folder', required: false},
    ],
    nodeNames: [],
    tasks: [
        {
            connections: [
                {
                    componentName: 'googleMail',
                    componentVersion: 1,
                    key: 'gmail',
                    required: true,
                    workflowNodeName: 'fetchEmails',
                },
            ],
            label: 'Fetch Emails',
            name: 'fetchEmails',
            type: 'codeWorkflow/v1/perform',
        },
        {
            connections: [
                {
                    componentName: 'googleMail',
                    componentVersion: 1,
                    key: 'googleMail',
                    required: true,
                    workflowNodeName: 'googleMail_1',
                },
                {
                    componentName: 'slack',
                    componentVersion: 1,
                    key: 'slack',
                    required: false,
                    workflowNodeName: 'googleMail_1',
                },
            ],
            name: 'googleMail_1',
            type: 'googleMail/v1/sendEmail',
        },
    ],
} as unknown as WorkflowDataStoreWorkflowType;

describe('useRun', () => {
    beforeEach(() => {
        workflowTestConfigurationRef.current = undefined;

        useWorkflowDataStore.setState({componentDefinitions, workflow});
    });

    it('should name each missing required input and connection', () => {
        const {result} = renderHook(() => useRun());

        expect(result.current.runDisabled).toBe(true);
        expect(result.current.runDisabledReasons).toEqual([
            {key: 'input_sender', message: 'Set a test value for the "Sender" input.'},
            {key: 'input_subject', message: 'Set a test value for the "subject" input.'},
            {key: 'connection_fetchEmails_gmail', message: 'Pick a Gmail connection ("gmail") for "Fetch Emails".'},
            {key: 'connection_googleMail_1_googleMail', message: 'Pick a Gmail connection for "googleMail_1".'},
        ]);
    });

    it('should enable the run once the test configuration sets the required values', () => {
        workflowTestConfigurationRef.current = {
            connections: [
                {connectionId: 7, workflowConnectionKey: 'gmail', workflowNodeName: 'fetchEmails'},
                {connectionId: 7, workflowConnectionKey: 'googleMail', workflowNodeName: 'googleMail_1'},
            ],
            environmentId: 1,
            inputs: {sender: 'someone@example.com', subject: 'Hello'},
        } as WorkflowTestConfiguration;

        const {result} = renderHook(() => useRun());

        expect(result.current.runDisabled).toBe(false);
        expect(result.current.runDisabledReasons).toEqual([]);
    });

    it('should fall back to the component name when its definition is not loaded', () => {
        useWorkflowDataStore.setState({componentDefinitions: []});

        const {result} = renderHook(() => useRun());

        expect(result.current.runDisabledReasons).toContainEqual({
            key: 'connection_fetchEmails_gmail',
            message: 'Pick a googleMail connection ("gmail") for "Fetch Emails".',
        });
    });

    it('should ask for a task when the workflow has none', () => {
        useWorkflowDataStore.setState({
            workflow: {id: 'workflow-2', nodeNames: [], tasks: []} as unknown as WorkflowDataStoreWorkflowType,
        });

        const {result} = renderHook(() => useRun());

        expect(result.current.runDisabledReasons).toEqual([{key: 'tasks', message: 'Add at least one task.'}]);
    });
});
