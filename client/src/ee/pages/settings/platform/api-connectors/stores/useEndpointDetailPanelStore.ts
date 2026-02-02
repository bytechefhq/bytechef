import {ApiConnectorEndpoint} from '@/shared/middleware/graphql';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface EndpointDetailPanelStateI {
    apiConnectorName?: string;
    closePanel: () => void;
    isOpen: boolean;
    openPanel: (endpoint: ApiConnectorEndpoint, apiConnectorName: string, specification?: string) => void;
    selectedEndpoint?: ApiConnectorEndpoint;
    specification?: string;
}

export const useEndpointDetailPanelStore = create<EndpointDetailPanelStateI>()(
    devtools(
        (set) => ({
            apiConnectorName: undefined,

            closePanel: () => {
                set({isOpen: false});

                setTimeout(() => {
                    set({
                        apiConnectorName: undefined,
                        selectedEndpoint: undefined,
                        specification: undefined,
                    });
                }, 300);
            },

            isOpen: false,

            openPanel: (endpoint: ApiConnectorEndpoint, apiConnectorName: string, specification?: string) => {
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
