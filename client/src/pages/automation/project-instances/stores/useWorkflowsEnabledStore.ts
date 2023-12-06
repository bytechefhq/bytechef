import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface WorkflowsEnabledState {
    workflowEnabledMap: Map<string, boolean>;
    setWorkflowEnabled: (workflowId: string, enabled: boolean) => void;
}

export const useWorkflowsEnabledStore = create<WorkflowsEnabledState>()(
    devtools(
        (set) => ({
            setWorkflowEnabled: (workflowId, enabled) =>
                set(({workflowEnabledMap}) => ({
                    workflowEnabledMap: new Map<string, boolean>(workflowEnabledMap.set(workflowId, enabled)),
                })),
            workflowEnabledMap: new Map<string, boolean>(),
        }),
        {
            name: 'workflows-enabled',
        }
    )
);
