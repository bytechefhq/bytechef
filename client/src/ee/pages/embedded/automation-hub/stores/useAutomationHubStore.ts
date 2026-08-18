import {EmbedInitParamsI} from '@/ee/pages/embedded/shared/useEmbedHandshake';
import {create} from 'zustand';

export interface AutomationHubTabsI {
    automations: boolean;
    connections: boolean;
    newWorkflow: boolean;
}

export interface AutomationHubThemeI {
    borderRadius?: string;
    fontFamily?: string;
    mode?: 'dark' | 'light';
    primaryColor?: string;
}

interface AutomationHubStateI {
    connectionDialogAllowed: boolean;
    includeComponents?: string[];
    initialize: (params: EmbedInitParamsI) => void;
    initialized: boolean;
    sharedConnectionIds: number[];
    tabs: AutomationHubTabsI;
    theme: AutomationHubThemeI;
}

const DEFAULT_TABS: AutomationHubTabsI = {
    automations: true,
    connections: true,
    newWorkflow: true,
};

export const useAutomationHubStore = create<AutomationHubStateI>()((set) => ({
    connectionDialogAllowed: true,
    includeComponents: undefined,
    initialize: (params) =>
        set({
            connectionDialogAllowed: params.connectionDialogAllowed ?? true,
            includeComponents: params.includeComponents,
            initialized: true,
            sharedConnectionIds: params.sharedConnectionIds ?? [],
            tabs: {...DEFAULT_TABS, ...(params.tabs ?? {})},
            theme: params.theme ?? {},
        }),
    initialized: false,
    sharedConnectionIds: [],
    tabs: DEFAULT_TABS,
    theme: {},
}));
