/* eslint-disable sort-keys */
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {create} from 'zustand';

interface OpenIntegrationInstanceConfigurationWorkflowSheetPropsI {
    integrationInstanceConfigurationId: number;
    integrationInstanceConfigurationName?: string;
    integrationVersion?: number;
    workflow: Workflow;
}

interface IntegrationInstanceConfigurationWorkflowSheetStateI {
    integrationInstanceConfigurationId: number | undefined;
    integrationInstanceConfigurationName: string | undefined;
    integrationVersion: number | undefined;
    workflow: Workflow | undefined;
    openIntegrationInstanceConfigurationWorkflowSheet: (
        props: OpenIntegrationInstanceConfigurationWorkflowSheetPropsI
    ) => void;

    integrationInstanceConfigurationWorkflowSheetOpen: boolean;
    setIntegrationInstanceConfigurationWorkflowSheetOpen: (
        integrationInstanceConfigurationWorkflowSheetOpen: boolean
    ) => void;
}

export const useIntegrationInstanceConfigurationWorkflowSheetStore =
    create<IntegrationInstanceConfigurationWorkflowSheetStateI>()((set) => ({
        integrationInstanceConfigurationId: undefined,
        integrationInstanceConfigurationName: undefined,
        integrationVersion: undefined,
        workflow: undefined,
        openIntegrationInstanceConfigurationWorkflowSheet: ({
            integrationInstanceConfigurationId,
            integrationInstanceConfigurationName,
            integrationVersion,
            workflow,
        }) =>
            set((state) => ({
                ...state,
                integrationInstanceConfigurationId,
                integrationInstanceConfigurationName,
                integrationInstanceConfigurationWorkflowSheetOpen: true,
                integrationVersion,
                workflow,
            })),

        integrationInstanceConfigurationWorkflowSheetOpen: false,
        setIntegrationInstanceConfigurationWorkflowSheetOpen: (integrationInstanceConfigurationWorkflowSheetOpen) =>
            set((state) => ({
                ...state,
                integrationInstanceConfigurationWorkflowSheetOpen,
            })),
    }));

export default useIntegrationInstanceConfigurationWorkflowSheetStore;
