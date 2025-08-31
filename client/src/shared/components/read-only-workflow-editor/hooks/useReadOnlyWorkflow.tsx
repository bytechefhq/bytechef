import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useReadOnlyWorkflowStore from '../stores/useReadOnlyWorkflowStore';

const useReadOnlyWorkflow = () => {
    const {isReadOnlyWorkflowSheetOpen, setIsReadOnlyWorkflowSheetOpen, setWorkflow, workflow} =
        useReadOnlyWorkflowStore(
            useShallow((state) => ({
                isReadOnlyWorkflowSheetOpen: state.isReadOnlyWorkflowSheetOpen,
                setIsReadOnlyWorkflowSheetOpen: state.setIsReadOnlyWorkflowSheetOpen,
                setWorkflow: state.setWorkflow,
                workflow: state.workflow,
            }))
        );

    const workflowComponentNames = useMemo(
        () => [...(workflow?.workflowTriggerComponentNames ?? []), ...(workflow?.workflowTaskComponentNames ?? [])],
        [workflow?.workflowTriggerComponentNames, workflow?.workflowTaskComponentNames]
    );

    const {useGetComponentDefinitionsQuery} = useWorkflowEditor();

    const {data: componentDefinitions, isLoading: isComponentDefinitionsLoading} = useGetComponentDefinitionsQuery!(
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
