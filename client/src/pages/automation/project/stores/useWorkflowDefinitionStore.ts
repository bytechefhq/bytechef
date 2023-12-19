/* eslint-disable sort-keys */

import {ComponentDataType, WorkflowDefinitionType} from '@/types/types';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface WorkflowDefinitionState {
    componentData: Array<ComponentDataType>;
    setComponentData: (componentData: Array<ComponentDataType>) => void;

    workflowDefinitions: WorkflowDefinitionType;
    setWorkflowDefinitions: (workflowDefinition: WorkflowDefinitionType) => void;
}

const useWorkflowDefinitionStore = create<WorkflowDefinitionState>()(
    devtools(
        persist(
            (set) => ({
                componentData: [],
                setComponentData: (componentData) => set(() => ({componentData})),

                workflowDefinitions: {},
                setWorkflowDefinitions: (workflowDefinitions) => set(() => ({workflowDefinitions})),
            }),
            {
                name: 'workflow-definition',
            }
        )
    )
);

export default useWorkflowDefinitionStore;
