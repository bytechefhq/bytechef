/* eslint-disable sort-keys */
import {create} from 'zustand';

interface IntegrationInstanceConfigurationWorkflowSheetStateI {
    integrationInstanceConfigurationWorkflowSheetOpen: boolean;
    setIntegrationInstanceConfigurationWorkflowSheetOpen: (
        integrationInstanceConfigurationWorkflowSheetOpen: boolean
    ) => void;

    workflowId: string | undefined;
    setWorkflowId: (workflowExecutionId: string | undefined) => void;

    workflowVersion: number | undefined;
    setWorkflowVersion: (setWorkflowVersion: number | undefined) => void;
}

export const useIntegrationInstanceConfigurationWorkflowSheetStore =
    create<IntegrationInstanceConfigurationWorkflowSheetStateI>()((set) => ({
        workflowId: undefined,
        setWorkflowId: (workflowId) =>
            set((state) => ({
                ...state,
                workflowId,
            })),

        workflowVersion: undefined,
        setWorkflowVersion: (workflowVersion) =>
            set((state) => ({
                ...state,
                workflowVersion,
            })),

        integrationInstanceConfigurationWorkflowSheetOpen: false,
        setIntegrationInstanceConfigurationWorkflowSheetOpen: (integrationInstanceConfigurationWorkflowSheetOpen) =>
            set((state) => ({
                ...state,
                integrationInstanceConfigurationWorkflowSheetOpen,
            })),
    }));

export default useIntegrationInstanceConfigurationWorkflowSheetStore;
