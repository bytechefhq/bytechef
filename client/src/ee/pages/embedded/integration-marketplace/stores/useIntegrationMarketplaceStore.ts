import {AutomationHubThemeI} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {EmbedInitParamsI} from '@/ee/pages/embedded/shared/useEmbedHandshake';
import {create} from 'zustand';

interface IntegrationMarketplaceStateI {
    initialize: (params: EmbedInitParamsI) => void;
    initialized: boolean;
    theme: AutomationHubThemeI;
}

export const useIntegrationMarketplaceStore = create<IntegrationMarketplaceStateI>()((set) => ({
    initialize: (params) =>
        set({
            initialized: true,
            theme: params.theme ?? {},
        }),
    initialized: false,
    theme: {},
}));
