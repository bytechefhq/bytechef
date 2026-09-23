import {EmbedInitParamsI} from '@/ee/pages/embedded/shared/useEmbedHandshake';
import {create} from 'zustand';

export interface AutomationHubTabsI {
    automations: boolean;
    connections: boolean;
    newWorkflow: boolean;
}

type AutomationHubLayoutType = 'grid' | 'list';

export interface AutomationHubThemeI {
    activeBorderColor?: string;
    borderRadius?: string;
    cardColor?: string;
    cssVariables?: Record<string, string>;
    disableColor?: string;
    enableColor?: string;
    fontFamily?: string;
    mode?: 'dark' | 'light';
    onAccentColor?: string;
    primaryColor?: string;
    segmentColor?: string;
    surfaceColor?: string;
}

interface AutomationHubStateI {
    connectionDialogAllowed: boolean;
    defaultLayout: AutomationHubLayoutType;
    editWorkflowAllowed: boolean;
    includeComponents?: string[];
    initialize: (params: EmbedInitParamsI) => void;
    initialized: boolean;
    layoutSwitcherAllowed: boolean;
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
    defaultLayout: 'grid',
    editWorkflowAllowed: true,
    includeComponents: undefined,
    initialize: (params) =>
        set({
            connectionDialogAllowed: params.connectionDialogAllowed ?? true,
            defaultLayout: params.defaultLayout ?? 'grid',
            editWorkflowAllowed: params.editWorkflowAllowed ?? true,
            includeComponents: params.includeComponents,
            initialized: true,
            layoutSwitcherAllowed: params.layoutSwitcherAllowed ?? true,
            tabs: {...DEFAULT_TABS, ...params.tabs},
            theme: params.theme ?? {},
        }),
    initialized: false,
    layoutSwitcherAllowed: true,
    tabs: DEFAULT_TABS,
    theme: {},
}));
