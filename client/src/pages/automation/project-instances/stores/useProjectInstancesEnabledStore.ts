import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface ProjectInstancesEnabledState {
    projectInstanceMap: Map<number, boolean>;
    setProjectInstanceEnabled: (projectInstanceId: number, enabled: boolean) => void;
}

export const useProjectInstancesEnabledStore = create<ProjectInstancesEnabledState>()(
    devtools(
        (set) => ({
            projectInstanceMap: new Map<number, boolean>(),
            setProjectInstanceEnabled: (projectInstanceId, enabled) =>
                set(({projectInstanceMap}) => ({
                    projectInstanceMap: new Map<number, boolean>(projectInstanceMap.set(projectInstanceId, enabled)),
                })),
        }),
        {
            name: 'project-instances-enabled',
        }
    )
);
