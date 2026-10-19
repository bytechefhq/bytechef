/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures use 'use' callback, not React hooks */
import {test as base} from '@playwright/test';

import {
    type ConnectionComponentNameType,
    type DeployedProjectI,
    type DeployedWorkflowI,
    type WorkflowDefinitionType,
    createConnection,
    createProjectViaApi,
    deleteConnection,
    deleteDeployment,
    deleteProjectViaApi,
    deployProject,
    getProjectWorkflows,
    getProjectWorkspaceId,
    importWorkflowDefinition,
} from '../utils/deploymentUtils';
import {type JobI, getJob, getProjectJobs, postWebhook, waitForNewProjectJob} from '../utils/jobUtils';
import {type ProjectFixturesType} from './project';

interface DeployOptionsI {
    enabledWorkflowLabels?: string[];
    projectId?: string;
}

export interface WorkflowDeployerI {
    createProject: () => Promise<string>;
    deploy: (definitions: WorkflowDefinitionType[], options?: DeployOptionsI) => Promise<DeployedProjectI>;
    getWorkflowUuid: (projectId: string, label: string) => Promise<string>;
    importWorkflows: (projectId: string, definitions: WorkflowDefinitionType[]) => Promise<void>;
    triggerWebhookAndWaitForJob: (
        deployedProject: DeployedProjectI,
        body?: unknown,
        workflowLabel?: string
    ) => Promise<JobI>;
    workspaceId: number;
}

export type WorkflowDeploymentFixturesType = {
    deployer: WorkflowDeployerI;
};

/**
 * Imports workflow definitions into the test's project through the API, publishes the project and deploys it to
 * the Development environment with test connections bound (created on demand). Deployments, extra projects and
 * connections are removed again after the test.
 */
export const workflowDeploymentTest = base.extend<WorkflowDeploymentFixturesType & ProjectFixturesType>({
    deployer: async ({page, project}, use) => {
        const workspaceId = await getProjectWorkspaceId(page, project.id);

        const connectionIds = new Map<ConnectionComponentNameType, number>();
        const deploymentIds: number[] = [];
        const extraProjectIds: string[] = [];

        const resolveConnectionId = async (componentName: ConnectionComponentNameType): Promise<number> => {
            let connectionId = connectionIds.get(componentName);

            if (connectionId === undefined) {
                connectionId = await createConnection(page, workspaceId, componentName);

                connectionIds.set(componentName, connectionId);
            }

            return connectionId;
        };

        const importWorkflows = async (projectId: string, definitions: WorkflowDefinitionType[]) => {
            for (const definition of definitions) {
                await importWorkflowDefinition(page, projectId, definition);
            }
        };

        const deployer: WorkflowDeployerI = {
            createProject: async () => {
                const projectId = await createProjectViaApi(page, workspaceId);

                extraProjectIds.push(projectId);

                return projectId;
            },
            deploy: async (definitions, options = {}) => {
                const projectId = options.projectId ?? project.id;

                await importWorkflows(projectId, definitions);

                const deployedProject = await deployProject({
                    connectionIdResolver: resolveConnectionId,
                    enabledWorkflowLabels: options.enabledWorkflowLabels,
                    page,
                    projectId,
                });

                deploymentIds.push(deployedProject.deploymentId);

                return deployedProject;
            },
            getWorkflowUuid: async (projectId, label) => {
                const projectWorkflows = await getProjectWorkflows(page, projectId);

                const projectWorkflow = projectWorkflows.find((workflow) => workflow.label === label);

                if (!projectWorkflow) {
                    throw new Error(`Project ${projectId} has no workflow labelled ${label}`);
                }

                return projectWorkflow.workflowUuid;
            },
            importWorkflows,
            triggerWebhookAndWaitForJob: async (deployedProject, body = {}, workflowLabel) => {
                const deployedWorkflow: DeployedWorkflowI | undefined = workflowLabel
                    ? deployedProject.workflows.find((workflow) => workflow.label === workflowLabel)
                    : deployedProject.workflows[0];

                if (!deployedWorkflow) {
                    throw new Error(`No deployed workflow labelled ${workflowLabel}`);
                }

                const projectJobs = await getProjectJobs(page, workspaceId, deployedProject.projectId);

                const webhookResponse = await postWebhook(page.request, deployedWorkflow.webhookUrl, body);

                if (!webhookResponse.ok()) {
                    throw new Error(`Webhook answered ${webhookResponse.status()}: ${await webhookResponse.text()}`);
                }

                const jobId = await waitForNewProjectJob({
                    knownJobIds: projectJobs.map((job) => job.id),
                    page,
                    projectId: deployedProject.projectId,
                    workspaceId,
                });

                return getJob(page, jobId);
            },
            workspaceId,
        };

        await use(deployer);

        for (const deploymentId of deploymentIds) {
            await deleteDeployment(page, deploymentId).catch(() => undefined);
        }

        for (const extraProjectId of extraProjectIds) {
            await deleteProjectViaApi(page, extraProjectId).catch(() => undefined);
        }

        for (const connectionId of connectionIds.values()) {
            await deleteConnection(page, connectionId).catch(() => undefined);
        }
    },
});
