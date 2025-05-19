import {Workflow} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useMemo} from 'react';

import useReadOnlyWorkflowStore from '../stores/useReadOnlyWorkflowStore';

const useReadOnlyWorkflow = () => {
    const {isReadOnlyWorkflowSheetOpen, setIsReadOnlyWorkflowSheetOpen, setWorkflow, workflow} =
        useReadOnlyWorkflowStore();

    const workflowComponentNames = useMemo(
        () => [...(workflow?.workflowTriggerComponentNames ?? []), ...(workflow?.workflowTaskComponentNames ?? [])],
        [workflow?.workflowTriggerComponentNames, workflow?.workflowTaskComponentNames]
    );

    const {data: componentDefinitions, isLoading: isComponentDefinitionsLoading} = useGetComponentDefinitionsQuery(
        {include: workflowComponentNames},
        workflowComponentNames !== undefined
    );

    function closeReadOnlyWorkflowSheet() {
        setIsReadOnlyWorkflowSheetOpen(false);
    }

    function openReadOnlyWorkflowSheet(workflow: Workflow) {
        if (!workflow) {
            return;
        }

        setWorkflow(workflow);

        setIsReadOnlyWorkflowSheetOpen(true);
    }

    const isLoading = isComponentDefinitionsLoading || !componentDefinitions || !workflow;

    return {
        closeReadOnlyWorkflowSheet,
        isLoading,
        isReadOnlyWorkflowSheetOpen,
        openReadOnlyWorkflowSheet,
        workflow,
    };
};

export default useReadOnlyWorkflow;
