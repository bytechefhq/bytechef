/* eslint-disable sort-keys */

import {ComponentDataType} from '@/types/types';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface WorkflowDefinitionState {
    componentData: Array<ComponentDataType>;
    setComponentData: (componentData: Array<ComponentDataType>) => void;
}

const useWorkflowDefinitionStore = create<WorkflowDefinitionState>()(
    devtools(
        persist(
            (set) => ({
                componentData: [],
                setComponentData: (componentData) =>
                    set(() => ({componentData})),
            }),
            {
                name: 'workflow-definition',
            }
        )
    )
);

export default useWorkflowDefinitionStore;
