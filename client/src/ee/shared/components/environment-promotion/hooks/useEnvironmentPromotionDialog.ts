import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {
    EnvironmentPromotionPreviewQuery,
    PromotionResourceType,
    useEnvironmentPromotionPreviewQuery,
    useEnvironmentsQuery,
    usePromoteToEnvironmentMutation,
} from '@/shared/middleware/graphql';
import {useEffect, useMemo, useState} from 'react';

export interface EnvironmentOptionI {
    id: number;
    label: string;
}

export type EnvironmentPromotionPreviewConnectionType =
    EnvironmentPromotionPreviewQuery['environmentPromotionPreview']['connections'][number];

export type EnvironmentPromotionPreviewType = EnvironmentPromotionPreviewQuery['environmentPromotionPreview'];

export interface UseEnvironmentPromotionDialogProps {
    resourceType: PromotionResourceType;
    sourceEnvironmentId: number;
    sourceId: string;
}

export const computeDefaultTargetEnvironmentId = (
    environmentIds: number[],
    sourceEnvironmentId: number
): number | undefined => {
    if (environmentIds.length === 0) {
        return undefined;
    }

    const sortedEnvironmentIds = [...environmentIds].sort((a, b) => a - b);

    const sourceIndex = sortedEnvironmentIds.indexOf(sourceEnvironmentId);

    if (sourceIndex === -1) {
        return sortedEnvironmentIds[0];
    }

    for (let offset = 1; offset <= sortedEnvironmentIds.length; offset++) {
        const candidateId = sortedEnvironmentIds[(sourceIndex + offset) % sortedEnvironmentIds.length];

        if (candidateId !== sourceEnvironmentId) {
            return candidateId;
        }
    }

    return undefined;
};

export function useEnvironmentPromotionDialog({
    resourceType,
    sourceEnvironmentId,
    sourceId,
}: UseEnvironmentPromotionDialogProps) {
    const [mappings, setMappings] = useState<Record<string, string | undefined>>({});
    const [targetEnvironmentId, setTargetEnvironmentId] = useState<number | undefined>(undefined);

    const {data: environmentsData} = useEnvironmentsQuery();

    const promoteMutation = usePromoteToEnvironmentMutation();

    const targetEnvironmentOptions = useMemo<EnvironmentOptionI[]>(() => {
        if (!environmentsData?.environments) {
            return [];
        }

        return environmentsData.environments.reduce<EnvironmentOptionI[]>((options, environment) => {
            if (environment?.id == null || +environment.id === sourceEnvironmentId) {
                return options;
            }

            const id = +environment.id;

            options.push({id, label: ENVIRONMENT_CONFIGS[id]?.label ?? environment.name});

            return options;
        }, []);
    }, [environmentsData?.environments, sourceEnvironmentId]);

    const previewQueryResult = useEnvironmentPromotionPreviewQuery(
        {
            resourceType,
            sourceId,
            targetEnvironmentId: targetEnvironmentId ?? 0,
        },
        {enabled: targetEnvironmentId != null}
    );

    const preview = previewQueryResult.data?.environmentPromotionPreview;

    const unresolvedConnectionCount = useMemo(
        () => Object.values(mappings).filter((targetConnectionId) => targetConnectionId == null).length,
        [mappings]
    );

    const handleTargetConnectionIdChange = (sourceConnectionId: string, targetConnectionId: string | undefined) => {
        setMappings((currentMappings) => ({
            ...currentMappings,
            [sourceConnectionId]: targetConnectionId,
        }));
    };

    const handlePromote = async () => {
        if (targetEnvironmentId == null) {
            return undefined;
        }

        const connectionMappings = Object.entries(mappings)
            .filter((entry): entry is [string, string] => entry[1] != null)
            .map(([sourceConnectionId, targetConnectionId]) => ({sourceConnectionId, targetConnectionId}));

        const {promoteToEnvironment} = await promoteMutation.mutateAsync({
            input: {
                connectionMappings,
                resourceType,
                sourceId,
                targetEnvironmentId: targetEnvironmentId.toString(),
            },
        });

        return promoteToEnvironment;
    };

    useEffect(() => {
        if (targetEnvironmentId != null || !environmentsData?.environments?.length) {
            return;
        }

        const environmentIds = environmentsData.environments
            .filter((environment): environment is {id: string; name: string} => environment != null)
            .map((environment) => +environment.id);

        const defaultTargetEnvironmentId = computeDefaultTargetEnvironmentId(environmentIds, sourceEnvironmentId);

        if (defaultTargetEnvironmentId != null) {
            setTargetEnvironmentId(defaultTargetEnvironmentId);
        }
    }, [environmentsData?.environments, sourceEnvironmentId, targetEnvironmentId]);

    useEffect(() => {
        if (!preview) {
            return;
        }

        setMappings(
            preview.connections.reduce<Record<string, string | undefined>>((nextMappings, connection) => {
                nextMappings[connection.sourceConnectionId] = connection.suggestedTargetConnectionId ?? undefined;

                return nextMappings;
            }, {})
        );
    }, [preview]);

    return {
        handlePromote,
        handleTargetConnectionIdChange,
        isPreviewLoading: previewQueryResult.isLoading,
        isPromotePending: promoteMutation.isPending,
        mappings,
        preview,
        setTargetEnvironmentId,
        targetEnvironmentId,
        targetEnvironmentOptions,
        unresolvedConnectionCount,
    };
}
