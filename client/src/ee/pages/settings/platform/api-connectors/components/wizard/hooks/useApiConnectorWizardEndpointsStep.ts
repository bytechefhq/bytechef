import {useCallback, useState} from 'react';

import {useApiConnectorWizardStore} from '../../../stores/useApiConnectorWizardStore';
import {EndpointDefinitionI} from '../../../types/api-connector-wizard.types';

interface UseApiConnectorWizardEndpointsStepI {
    editingEndpoint: EndpointDefinitionI | undefined;
    endpoints: EndpointDefinitionI[];
    handleCloseDialog: () => void;
    handleSaveEndpoint: (endpoint: EndpointDefinitionI) => void;
    isDialogOpen: boolean;
    openAddDialog: () => void;
    openEditDialog: (endpoint: EndpointDefinitionI) => void;
    removeEndpoint: (id: string) => void;
}

export default function useApiConnectorWizardEndpointsStep(): UseApiConnectorWizardEndpointsStepI {
    const {addEndpoint, endpoints, removeEndpoint, updateEndpoint} = useApiConnectorWizardStore();
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingEndpoint, setEditingEndpoint] = useState<EndpointDefinitionI | undefined>(undefined);

    const openAddDialog = useCallback(() => {
        setEditingEndpoint(undefined);
        setIsDialogOpen(true);
    }, []);

    const openEditDialog = useCallback((endpoint: EndpointDefinitionI) => {
        setEditingEndpoint(endpoint);
        setIsDialogOpen(true);
    }, []);

    const handleSaveEndpoint = useCallback(
        (endpoint: EndpointDefinitionI) => {
            if (editingEndpoint) {
                updateEndpoint(editingEndpoint.id, endpoint);
            } else {
                addEndpoint(endpoint);
            }

            setIsDialogOpen(false);
            setEditingEndpoint(undefined);
        },
        [addEndpoint, editingEndpoint, updateEndpoint]
    );

    const handleCloseDialog = useCallback(() => {
        setIsDialogOpen(false);
        setEditingEndpoint(undefined);
    }, []);

    return {
        editingEndpoint,
        endpoints,
        handleCloseDialog,
        handleSaveEndpoint,
        isDialogOpen,
        openAddDialog,
        openEditDialog,
        removeEndpoint,
    };
}
