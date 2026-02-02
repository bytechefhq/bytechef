import {ApiConnectorEndpoint} from '@/shared/middleware/graphql';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

// Duration to wait before clearing panel state after closing.
// Matches the Sheet component's slide-out animation duration to prevent visual flickering.
const PANEL_CLOSE_ANIMATION_DELAY_MS = 300;

interface EndpointDetailPanelStateI {
    apiConnectorName?: string;
    closePanel: () => void;
    closePanelTimeoutId: ReturnType<typeof setTimeout> | null;
    isOpen: boolean;
    openPanel: (endpoint: ApiConnectorEndpoint, apiConnectorName: string, specification?: string) => void;
    selectedEndpoint?: ApiConnectorEndpoint;
    specification?: string;
}

export const useEndpointDetailPanelStore = create<EndpointDetailPanelStateI>()(
    devtools(
        (set, get) => ({
            apiConnectorName: undefined,

            closePanel: () => {
                const {closePanelTimeoutId} = get();

                if (closePanelTimeoutId) {
                    clearTimeout(closePanelTimeoutId);
                }

                set({isOpen: false});

                const timeoutId = setTimeout(() => {
                    set({
                        apiConnectorName: undefined,
                        closePanelTimeoutId: null,
                        selectedEndpoint: undefined,
                        specification: undefined,
                    });
                }, PANEL_CLOSE_ANIMATION_DELAY_MS);

                set({closePanelTimeoutId: timeoutId});
            },

            closePanelTimeoutId: null,

            isOpen: false,

            openPanel: (endpoint: ApiConnectorEndpoint, apiConnectorName: string, specification?: string) => {
                const {closePanelTimeoutId} = get();

                if (closePanelTimeoutId) {
                    clearTimeout(closePanelTimeoutId);
                    set({closePanelTimeoutId: null});
                }

                set({
                    apiConnectorName,
                    isOpen: true,
                    selectedEndpoint: endpoint,
                    specification,
                });
            },

            selectedEndpoint: undefined,
            specification: undefined,
        }),
        {
            name: 'bytechef.endpoint-detail-panel',
        }
    )
);
