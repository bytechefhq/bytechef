import {VARIABLES_NODE_NAME} from '@/pages/platform/workflow-editor/utils/getWorkflowInputAndVariableDataPills';
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

    // Mention display for a variable pill is `vars.NAME` (optionally wrapped as `${vars.NAME}` in formula-mode
    // text) — it has no underscore-delimited component/index/operation shape, so it must be special-cased before
    // the generic parsing below, which would otherwise leave it unmatched and fall through to the default icon.
    const unwrappedMentionDisplay = mentionDisplay?.replace('${', '').replace(/}$/, '') ?? '';

    if (
        unwrappedMentionDisplay === VARIABLES_NODE_NAME ||
        unwrappedMentionDisplay.startsWith(`${VARIABLES_NODE_NAME}.`)
    ) {
        const svgString = renderToStaticMarkup(TYPE_ICONS.VARIABLE);

        return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svgString)}`;
    }

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
