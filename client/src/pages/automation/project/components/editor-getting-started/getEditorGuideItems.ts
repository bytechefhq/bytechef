import {AutomationGettingStartedStatusType} from '@/pages/automation/getting-started/automationGettingStartedStatus';
import {hasFirstRun} from '@/pages/automation/getting-started/getAutomationGettingStartedSteps';
import {forEachNestedTaskGroup} from '@/pages/platform/workflow-editor/utils/taskTraversalUtils';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';

export type EditorGuideItemIdType = 'fetch' | 'test' | 'log' | 'publish' | 'deploy' | 'enable' | 'run' | 'executions';

export interface EditorGuideItemI {
    copyLabel?: string;
    copyValue?: string;
    done: boolean;
    hint: string;
    id: EditorGuideItemIdType;
    label: string;
}

export interface EditorGuideInputI {
    deployment?: ProjectDeployment;
    executionsViewed: boolean;
    guideProjectPublished: boolean;
    httpClientTested: boolean;
    status?: AutomationGettingStartedStatusType;
    tasks: WorkflowTask[];
}

export function collectTasks(tasks: WorkflowTask[]): WorkflowTask[] {
    const collectedTasks: WorkflowTask[] = [];

    for (const task of tasks) {
        collectedTasks.push(task);

        if (task.parameters && typeof task.parameters === 'object') {
            forEachNestedTaskGroup(task.parameters as Record<string, unknown>, (nestedTasks) => {
                collectedTasks.push(...collectTasks(nestedTasks));
            });
        }
    }

    return collectedTasks;
}

const hasTextParameter = (task: WorkflowTask, parameterName: string) => {
    const parameterValue = (task.parameters as Record<string, unknown> | undefined)?.[parameterName];

    return typeof parameterValue === 'string' && parameterValue.trim() !== '';
};

export function isDeploymentLive(deployment?: ProjectDeployment): boolean {
    if (!deployment?.enabled) {
        return false;
    }

    const deploymentWorkflows = deployment.projectDeploymentWorkflows ?? [];

    return deploymentWorkflows.length > 0 && deploymentWorkflows.every((workflow) => workflow.enabled);
}

export function getEditorGuideItems({
    deployment,
    executionsViewed,
    guideProjectPublished,
    httpClientTested,
    status,
    tasks,
}: EditorGuideInputI): EditorGuideItemI[] {
    const firstRun = hasFirstRun(status);
    const published = !!status?.published && guideProjectPublished;

    return [
        {
            copyValue: 'https://dummyjson.com/quotes/random',
            done:
                firstRun ||
                published ||
                tasks.some(
                    (task) =>
                        task.type.startsWith('httpClient/') &&
                        task.type.endsWith('/get') &&
                        hasTextParameter(task, 'uri')
                ),
            hint: 'Click + below the trigger → HTTP Client → GET, paste the URI',
            id: 'fetch',
            label: 'Fetch a quote',
        },
        {
            done: firstRun || httpClientTested || published,
            hint: "Open the HTTP Client step's Output tab → Test Action",
            id: 'test',
            label: 'Test it',
        },
        {
            copyLabel: 'Or paste',
            copyValue: '${httpClient_1.quote}',
            done:
                firstRun ||
                published ||
                tasks.some((task) => task.type.startsWith('logger/') && hasTextParameter(task, 'text')),
            hint: 'Click + below HTTP Client → Helpers tab → Logger → Info, click into Text, pick quote under HTTP Client in the Data Pill Panel, then click Test',
            id: 'log',
            label: 'Log it',
        },
        {
            done: firstRun || published,
            hint: 'Freezes a version you can deploy',
            id: 'publish',
            label: 'Publish',
        },
        {
            done: firstRun || (!!status?.deployed && deployment !== undefined),
            hint: 'Switch on the workflow, then Save',
            id: 'deploy',
            label: 'Deploy in Development',
        },
        {
            done: firstRun || (!!status?.live && isDeploymentLive(deployment)),
            hint: 'Switches on the project in Deployments, with its workflow',
            id: 'enable',
            label: 'Enable',
        },
        {
            done: firstRun,
            hint: 'Click Run now to run the deployed workflow',
            id: 'run',
            label: 'See it run',
        },
        {
            done: executionsViewed,
            hint: 'In Deployments, click the workflow, or click View executions',
            id: 'executions',
            label: 'See executions',
        },
    ];
}
