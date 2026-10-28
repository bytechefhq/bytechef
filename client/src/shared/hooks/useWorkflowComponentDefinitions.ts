import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import {useGetComponentDefinitionsQuery as useGetAutomationComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {GetComponentDefinitionsRequestI} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {UseQueryResult} from '@tanstack/react-query';
import {useMemo} from 'react';

type WorkflowComponentDefinitionsRecordType = Record<string, WorkflowComponentIconDefinitionType | undefined>;

interface DefinitionI {
    icon?: string;
    name?: string;
    title?: string;
}

type UseGetComponentDefinitionsQueryType = (
    request: GetComponentDefinitionsRequestI,
    enabled?: boolean
) => UseQueryResult<Array<DefinitionI>, Error>;

interface WorkflowComponentNamesSourceI {
    workflowTaskComponentNames?: Array<string>;
    workflowTriggerComponentNames?: Array<string>;
}

/**
 * Trigger component names first, then task component names, each name once. The order matters: the workflow trigger
 * and components row slices the trigger names off the front.
 */
export const getWorkflowComponentNames = (workflow?: WorkflowComponentNamesSourceI) => {
    const componentNames = [
        ...(workflow?.workflowTriggerComponentNames ?? []),
        ...(workflow?.workflowTaskComponentNames ?? []),
    ];

    return componentNames.filter((componentName, index) => componentNames.indexOf(componentName) === index);
};

const toRecord = (definitions?: Array<DefinitionI>): WorkflowComponentDefinitionsRecordType =>
    Object.fromEntries(
        (definitions ?? [])
            .filter((definition) => definition.name != null)
            .map((definition) => [
                definition.name!,
                {icon: definition.icon, name: definition.name!, title: definition.title},
            ])
    );

/**
 * Component and task dispatcher definitions keyed by name, in the shape the workflow trigger and components row
 * expects. Both queries are shared through react-query, so every row on a page reuses one fetch. Pass the embedded
 * component definitions query hook from embedded pages; the automation one is the default.
 */
export default function useWorkflowComponentDefinitions(
    useGetComponentDefinitionsQuery: UseGetComponentDefinitionsQueryType = useGetAutomationComponentDefinitionsQuery
) {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });
    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const workflowComponentDefinitions = useMemo(() => toRecord(componentDefinitions), [componentDefinitions]);

    const workflowTaskDispatcherDefinitions = useMemo(
        () => toRecord(taskDispatcherDefinitions),
        [taskDispatcherDefinitions]
    );

    return {workflowComponentDefinitions, workflowTaskDispatcherDefinitions};
}
