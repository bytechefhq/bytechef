import {ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {describe, expect, it} from 'vitest';

import getWorkflowComponentConnections, {buildDeploymentWorkflows} from './projectDeploymentDialog-utils';

const workflows = [
    {
        id: 'workflow-1',
        tasks: [
            {
                connections: [
                    {
                        componentName: 'httpClient',
                        componentVersion: 1,
                        key: 'httpClient_1',
                        required: false,
                        workflowNodeName: 'httpClient_1',
                    },
                ],
                name: 'httpClient_1',
                type: 'httpClient/v1/get',
            },
        ],
        workflowUuid: 'workflow-uuid-1',
    },
] as unknown as Workflow[];

const buildFormWorkflow = (connectionId: number | undefined): ProjectDeploymentWorkflow[] =>
    [
        {
            connections: [
                {
                    connectionId,
                    workflowConnectionKey: 'httpClient_1',
                    workflowNodeName: 'httpClient_1',
                },
            ],
            enabled: true,
            inputs: {},
            workflowId: 'workflow-1',
        },
    ] as unknown as ProjectDeploymentWorkflow[];

describe('buildDeploymentWorkflows', () => {
    it('should drop a connection the user never selected', () => {
        const deploymentWorkflows = buildDeploymentWorkflows(buildFormWorkflow(undefined), workflows);

        expect(deploymentWorkflows?.[0].connections).toEqual([]);
    });

    it('should drop a connection the user cleared, which the select reports as NaN', () => {
        const deploymentWorkflows = buildDeploymentWorkflows(buildFormWorkflow(Number('null')), workflows);

        expect(deploymentWorkflows?.[0].connections).toEqual([]);
    });

    it('should keep a selected connection', () => {
        const deploymentWorkflows = buildDeploymentWorkflows(buildFormWorkflow(42), workflows);

        expect(deploymentWorkflows?.[0].connections).toEqual([
            {
                connectionId: 42,
                workflowConnectionKey: 'httpClient_1',
                workflowNodeName: 'httpClient_1',
            },
        ]);
    });
});

const buildTaskConnection = (workflowNodeName: string) => ({
    componentName: 'oracle',
    componentVersion: 1,
    key: workflowNodeName,
    required: true,
    workflowNodeName,
});

const workflowWithDisabledTasks = {
    definition: JSON.stringify({
        tasks: [
            {name: 'oracle_1', type: 'oracle/v1/execute'},
            {disabled: true, name: 'oracle_2', type: 'oracle/v1/execute'},
            {
                disabled: true,
                name: 'loop_1',
                parameters: {items: [], iteratee: [{name: 'oracle_3', type: 'oracle/v1/insert'}]},
                type: 'loop/v1',
            },
        ],
    }),
    id: 'workflow-2',
    tasks: [
        {connections: [buildTaskConnection('oracle_1')], name: 'oracle_1', type: 'oracle/v1/execute'},
        {connections: [buildTaskConnection('oracle_2')], disabled: true, name: 'oracle_2', type: 'oracle/v1/execute'},
        {disabled: true, name: 'loop_1', type: 'loop/v1'},
        {connections: [buildTaskConnection('oracle_3')], name: 'oracle_3', type: 'oracle/v1/insert'},
    ],
    workflowUuid: 'workflow-uuid-2',
} as unknown as Workflow;

describe('getWorkflowComponentConnections', () => {
    it('should skip connections of disabled tasks and of tasks nested in a disabled task', () => {
        expect(
            getWorkflowComponentConnections(workflowWithDisabledTasks).map(
                (componentConnection) => componentConnection.workflowNodeName
            )
        ).toEqual(['oracle_1']);
    });
});
