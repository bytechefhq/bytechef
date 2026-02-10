import {useImportOpenApiSpecificationMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';

export const API_CONNECTORS_PATH = '/automation/settings/api-connectors';

interface UseImportApiConnectorI {
    handleCancel: () => void;
    handleSave: () => void;
    isPending: boolean;
}

const useImportApiConnector = (): UseImportApiConnectorI => {
    const {icon, name, reset, specification} = useApiConnectorWizardStore();

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ['apiConnectors'],
        });

        reset();
        navigate(API_CONNECTORS_PATH);
    };

    const importOpenApiSpecificationMutation = useImportOpenApiSpecificationMutation({onSuccess});

    const handleSave = () => {
        if (!name || !specification) {
            return;
        }

        importOpenApiSpecificationMutation.mutate({
            input: {
                icon: icon || undefined,
                name,
                specification,
            },
        });
    };

    const handleCancel = () => {
        reset();
        navigate(API_CONNECTORS_PATH);
    };

    return {
        handleCancel,
        handleSave,
        isPending: importOpenApiSpecificationMutation.isPending,
    };
};

export default useImportApiConnector;
