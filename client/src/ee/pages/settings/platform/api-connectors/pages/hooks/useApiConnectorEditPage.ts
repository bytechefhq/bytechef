import {useApiConnectorQuery, useUpdateApiConnectorMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {toast} from 'sonner';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';
import {parseSpecificationForWizard} from '../../utils/specification-utils';
import {API_CONNECTORS_PATH} from './useImportApiConnector';

interface UseApiConnectorEditPageI {
    canProceed: boolean;
    currentStep: number;
    handleCancel: () => void;
    handleNext: () => void;
    handleSave: () => void;
    isHydrated: boolean;
    isPending: boolean;
    previousStep: () => void;
}

const useApiConnectorEditPage = (): UseApiConnectorEditPageI => {
    const [isHydrated, setIsHydrated] = useState(false);
    const [loadedVersion, setLoadedVersion] = useState<number | undefined>(undefined);

    const {
        currentStep,
        endpoints,
        icon,
        name,
        nextStep,
        previousStep,
        reset,
        setBaseSpecification,
        setBaseUrl,
        setEndpoints,
        setIcon,
        setName,
        specification,
    } = useApiConnectorWizardStore();

    const navigate = useNavigate();
    const {apiConnectorId} = useParams();
    const queryClient = useQueryClient();

    const {data: apiConnectorData} = useApiConnectorQuery({id: apiConnectorId || ''}, {enabled: !!apiConnectorId});

    const updateApiConnectorMutation = useUpdateApiConnectorMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['apiConnectors'],
            });
            queryClient.invalidateQueries({
                queryKey: ['apiConnector'],
            });

            reset();
            navigate(API_CONNECTORS_PATH);
        },
    });

    const handleNext = () => {
        nextStep();
    };

    // The update is id-addressed and version-checked server side, so renaming is safe and concurrent edits are
    // rejected instead of silently overwritten.
    const handleSave = () => {
        if (!apiConnectorId || !name || !specification) {
            return;
        }

        updateApiConnectorMutation.mutate({
            id: apiConnectorId,
            input: {
                icon: icon || undefined,
                name,
                specification,
                version: loadedVersion,
            },
        });
    };

    const handleCancel = () => {
        reset();
        navigate(API_CONNECTORS_PATH);
    };

    const canProceed = (() => {
        if (currentStep === 0) {
            return !!name;
        }

        return endpoints.length > 0;
    })();

    useEffect(() => {
        reset();
        setIsHydrated(false);
    }, [apiConnectorId, reset]);

    useEffect(() => {
        const apiConnector = apiConnectorData?.apiConnector;

        if (!apiConnector || isHydrated) {
            return;
        }

        const parsedSpecification = parseSpecificationForWizard(apiConnector.specification || '');

        if (!parsedSpecification) {
            toast.error('Failed to load API connector', {
                description: 'The stored specification could not be parsed for editing.',
            });

            navigate(API_CONNECTORS_PATH);

            return;
        }

        setName(apiConnector.name);
        setIcon(apiConnector.icon || '');
        setBaseSpecification(apiConnector.specification || undefined);
        setEndpoints(parsedSpecification.endpoints);
        setLoadedVersion(apiConnector.version ?? undefined);

        if (parsedSpecification.baseUrl) {
            setBaseUrl(parsedSpecification.baseUrl);
        }

        setIsHydrated(true);
    }, [
        apiConnectorData?.apiConnector,
        isHydrated,
        navigate,
        setBaseSpecification,
        setBaseUrl,
        setEndpoints,
        setIcon,
        setName,
    ]);

    return {
        canProceed,
        currentStep,
        handleCancel,
        handleNext,
        handleSave,
        isHydrated,
        isPending: updateApiConnectorMutation.isPending,
        previousStep,
    };
};

export default useApiConnectorEditPage;
