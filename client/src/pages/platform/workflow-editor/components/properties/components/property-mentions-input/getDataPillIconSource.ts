import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {renderToStaticMarkup} from 'react-dom/server';

export interface GetDataPillIconSourceProps {
    componentDefinitions?: Array<ComponentDefinitionBasic>;
    mentionDisplay: string;
    taskDispatcherDefinitions?: Array<TaskDispatcherDefinitionBasic>;
    workflow: Workflow;
}

export function getDataPillIconSource({
    componentDefinitions,
    mentionDisplay,
    taskDispatcherDefinitions,
    workflow,
}: GetDataPillIconSourceProps): string {
    const definitions = componentDefinitions ?? [];
    const dispatchers = taskDispatcherDefinitions ?? [];

    let componentName = mentionDisplay?.split('_')[0].replace('${', '');

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

    const svgString = renderToStaticMarkup(TYPE_ICONS.STRING);

    return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svgString)}`;
}
