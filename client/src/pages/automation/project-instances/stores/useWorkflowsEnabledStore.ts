import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface WorkflowsEnabledStateI {
    reset: () => void;
    setWorkflowEnabled: (workflowId: string, enabled: boolean) => void;
    workflowEnabledMap: Map<string, boolean>;
}

export const useWorkflowsEnabledStore = create<WorkflowsEnabledStateI>()(
    devtools(
        (set) => ({
            reset: () =>
                set(() => ({
                    workflowEnabledMap: new Map<string, boolean>(),
                })),
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
