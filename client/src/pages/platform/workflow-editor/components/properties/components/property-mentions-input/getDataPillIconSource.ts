import {forEachNestedTaskGroup} from '@/pages/platform/workflow-editor/utils/taskTraversalUtils';
import {getWorkflowNodeComponentName} from '@/pages/platform/workflow-editor/utils/workflowNodeNameUtils';
import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {renderToStaticMarkup} from 'react-dom/server';

export interface GetDataPillIconSourceProps {
    componentDefinitions?: Array<ComponentDefinitionBasic>;
    mentionDisplay: string;
    taskDispatcherDefinitions?: Array<TaskDispatcherDefinitionBasic>;
    workflow: Workflow;
}

function getExpressionSegments(mentionDisplay: string): Array<string> {
    const expression = mentionDisplay.startsWith('${') ? mentionDisplay.substring(2) : mentionDisplay;

    return expression.replace(/}$/, '').split(/[.[]/);
}

function getTypeIconSource(type: keyof typeof TYPE_ICONS): string {
    const svgString = renderToStaticMarkup(TYPE_ICONS[type]);

    return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svgString)}`;
}

function getWorkflowInputType(workflow: Workflow, inputName: string | undefined): keyof typeof TYPE_ICONS | undefined {
    const inputType = workflow.inputs?.find((input) => input.name === inputName)?.type?.toUpperCase();

    return inputType && inputType in TYPE_ICONS ? (inputType as keyof typeof TYPE_ICONS) : undefined;
}

function findTaskType(tasks: Array<WorkflowTask> | undefined, nodeName: string): string | undefined {
    if (!tasks) {
        return undefined;
    }

    for (const task of tasks) {
        if (task.name === nodeName) {
            return task.type;
        }

        if (task.parameters) {
            let nestedType: string | undefined;

            forEachNestedTaskGroup(task.parameters as Record<string, unknown>, (nestedTasks) => {
                nestedType = nestedType ?? findTaskType(nestedTasks, nodeName);
            });

            if (nestedType) {
                return nestedType;
            }
        }
    }

    return undefined;
}

export function getWorkflowNodeComponentNameFromDefinition(workflow: Workflow, nodeName: string): string | undefined {
    const triggerType = workflow.triggers?.find((trigger) => trigger.name === nodeName)?.type;

    const type = triggerType ?? findTaskType(workflow.tasks, nodeName);

    return type?.split('/')[0];
}

export function getDataPillIconSource({
    componentDefinitions,
    mentionDisplay,
    taskDispatcherDefinitions,
    workflow,
}: GetDataPillIconSourceProps): string {
    const definitions = componentDefinitions ?? [];
    const dispatchers = taskDispatcherDefinitions ?? [];

    const [nodeName = '', inputName] = getExpressionSegments(mentionDisplay ?? '');

    if (nodeName === 'inputs') {
        return getTypeIconSource(getWorkflowInputType(workflow, inputName) ?? 'STRING');
    }

    let componentName =
        getWorkflowNodeComponentNameFromDefinition(workflow, nodeName) ?? getWorkflowNodeComponentName(nodeName);

    if (componentName === 'trigger') {
        componentName = workflow.workflowTriggerComponentNames?.[0] || '';
    }

    if (TASK_DISPATCHER_NAMES.includes(componentName)) {
        const icon = dispatchers.find((component) => component.name === componentName)?.icon;

        if (icon) {
            return icon;
        }
    }

    const componentIcon = definitions.find((component) => component.name === componentName)?.icon;

    if (componentIcon) {
        return componentIcon;
    }

    return getTypeIconSource('STRING');
}
