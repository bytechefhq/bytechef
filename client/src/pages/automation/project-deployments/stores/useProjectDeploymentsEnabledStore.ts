import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface ProjectDeploymentsEnabledStateI {
    projectDeploymentMap: Map<number, boolean>;
    setProjectDeploymentEnabled: (projectDeploymentId: number, enabled: boolean) => void;
}

export const useProjectDeploymentsEnabledStore = create<ProjectDeploymentsEnabledStateI>()(
    devtools(
        (set) => ({
            projectDeploymentMap: new Map<number, boolean>(),
            setProjectDeploymentEnabled: (projectDeploymentId, enabled) =>
                set(({projectDeploymentMap}) => ({
                    projectDeploymentMap: new Map<number, boolean>(
                        projectDeploymentMap.set(projectDeploymentId, enabled)
                    ),
                })),
        }),
        {
            name: 'project-deployments-enabled',
        }
    )
);
