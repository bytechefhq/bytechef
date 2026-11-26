import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {
    EnvironmentPromotionPreviewQuery,
    PromotionChangeKind,
    PromotionResourceType,
    PromotionSchemaChangeKind,
    useEnvironmentPromotionPreviewQuery,
    useEnvironmentsQuery,
    usePromoteToEnvironmentMutation,
} from '@/shared/middleware/graphql';
import {GraphQLResponseError} from '@/shared/middleware/graphqlFetcher';
import {QueryClient, useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo, useRef, useState} from 'react';

export interface EnvironmentOptionI {
    id: number;
    label: string;
}

export type EnvironmentPromotionPreviewConnectionType =
    EnvironmentPromotionPreviewQuery['environmentPromotionPreview']['connections'][number];

export type EnvironmentPromotionPreviewType = EnvironmentPromotionPreviewQuery['environmentPromotionPreview'];

export interface PromotionChangeRowI {
    assetType: string;
    destructive: boolean;
    key: string;
    kind: PromotionChangeKind;
    name: string;
    parentName: string | null;
}

export const PROMOTION_PREVIEW_STALE_MESSAGE = 'The target changed since the preview. Review the changes again.';

const ENVIRONMENT_PROMOTION_ERROR_ENTITY_CLASS = 'EnvironmentPromotionErrorType';

const PROMOTION_PREVIEW_STALE_ERROR_KEY = 110;

const SCHEMA_CHANGE_KINDS: Partial<Record<PromotionSchemaChangeKind, PromotionChangeKind>> = {
    [PromotionSchemaChangeKind.AddColumn]: PromotionChangeKind.Create,
    [PromotionSchemaChangeKind.ChangeColumnType]: PromotionChangeKind.Update,
    [PromotionSchemaChangeKind.ChangeLink]: PromotionChangeKind.Update,
    [PromotionSchemaChangeKind.DropColumn]: PromotionChangeKind.Delete,
};

export const isPromotionPreviewStaleError = (error: unknown): boolean =>
    error instanceof GraphQLResponseError &&
    error.extensions?.entityClass === ENVIRONMENT_PROMOTION_ERROR_ENTITY_CLASS &&
    error.extensions?.errorKey === PROMOTION_PREVIEW_STALE_ERROR_KEY;

export const getPromotionChangeRows = (preview: EnvironmentPromotionPreviewType): PromotionChangeRowI[] => {
    const columnRows = preview.schemaChanges.reduce<PromotionChangeRowI[]>((rows, schemaChange) => {
        const kind = SCHEMA_CHANGE_KINDS[schemaChange.kind];

        if (schemaChange.columnName == null || kind == null) {
            return rows;
        }

        rows.push({
            assetType: 'COLUMN',
            destructive: schemaChange.destructive,
            key: schemaChange.columnName,
            kind,
            name: schemaChange.columnName,
            parentName: schemaChange.tableName,
        });

        return rows;
    }, []);

    return [...preview.changes, ...columnRows];
};

export const countDestructiveChanges = (preview: EnvironmentPromotionPreviewType | undefined): number => {
    if (!preview) {
        return 0;
    }

    return (
        preview.changes.filter((change) => change.destructive).length +
        preview.schemaChanges.filter((schemaChange) => schemaChange.destructive).length
    );
};

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

    const sortedEnvironmentIds = [...environmentIds].sort(
        (firstEnvironmentId, secondEnvironmentId) => firstEnvironmentId - secondEnvironmentId
    );

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

const removeEnvironmentPromotionPreviewQueries = (
    queryClient: QueryClient,
    {resourceType, sourceEnvironmentId, sourceId}: UseEnvironmentPromotionDialogProps
) =>
    queryClient.removeQueries({
        queryKey: [
            'environmentPromotionPreview',
            {resourceType, sourceEnvironmentId: sourceEnvironmentId.toString(), sourceId},
        ],
    });

export function useEnvironmentPromotionDialog({
    resourceType,
    sourceEnvironmentId,
    sourceId,
}: UseEnvironmentPromotionDialogProps) {
    const [acknowledgedResetKey, setAcknowledgedResetKey] = useState<string | undefined>(undefined);
    const [isPreviewStale, setPreviewStale] = useState(false);
    const [isReturnToReviewPending, setReturnToReviewPending] = useState(false);
    const [mappings, setMappings] = useState<Record<string, string | undefined>>({});
    const [targetEnvironmentId, setTargetEnvironmentId] = useState<number | undefined>(undefined);

    const previousWizardResetKeyRef = useRef<string | undefined>(undefined);

    const queryClient = useQueryClient();

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
            sourceEnvironmentId: sourceEnvironmentId.toString(),
            sourceId,
            targetEnvironmentId: targetEnvironmentId ?? 0,
        },
        {
            enabled: targetEnvironmentId != null,
            refetchOnReconnect: false,
            refetchOnWindowFocus: false,
            staleTime: Infinity,
        }
    );

    const preview = previewQueryResult.data?.environmentPromotionPreview;

    const destructiveChangeCount = countDestructiveChanges(preview);

    const wizardResetKey = `${targetEnvironmentId}:${preview?.destructiveChangesHash || ''}`;

    const isDestructiveAcknowledged = acknowledgedResetKey === wizardResetKey;

    const targetEnvironmentLabel = useMemo(() => {
        if (targetEnvironmentId == null) {
            return '';
        }

        const targetEnvironmentOption = targetEnvironmentOptions.find((option) => option.id === targetEnvironmentId);

        return (
            ENVIRONMENT_CONFIGS[targetEnvironmentId]?.label ||
            targetEnvironmentOption?.label ||
            `environment ${targetEnvironmentId}`
        );
    }, [targetEnvironmentId, targetEnvironmentOptions]);

    const suggestedMappings = useMemo(
        () =>
            (preview?.connections ?? []).reduce<Record<string, string | undefined>>((nextMappings, connection) => {
                nextMappings[connection.sourceConnectionId] = connection.suggestedTargetConnectionId ?? undefined;

                return nextMappings;
            }, {}),
        [preview]
    );

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

    const handleTargetEnvironmentIdChange = (nextTargetEnvironmentId: number) => {
        setPreviewStale(false);
        setReturnToReviewPending(false);
        setTargetEnvironmentId(nextTargetEnvironmentId);
    };

    const completeReturnToReview = () => setReturnToReviewPending(false);

    const setDestructiveAcknowledged = (value: boolean) => setAcknowledgedResetKey(value ? wizardResetKey : undefined);

    const handlePromote = async () => {
        if (targetEnvironmentId == null) {
            return undefined;
        }

        setPreviewStale(false);
        setReturnToReviewPending(false);

        const connectionMappings = Object.entries(mappings)
            .filter((entry): entry is [string, string] => entry[1] != null && entry[1] !== suggestedMappings[entry[0]])
            .map(([sourceConnectionId, targetConnectionId]) => ({sourceConnectionId, targetConnectionId}));

        try {
            const {promoteToEnvironment} = await promoteMutation.mutateAsync({
                input: {
                    connectionMappings,
                    expectedDestructiveChangesHash: preview?.destructiveChangesHash,
                    resourceType,
                    sourceEnvironmentId: sourceEnvironmentId.toString(),
                    sourceId,
                    targetEnvironmentId: targetEnvironmentId.toString(),
                },
            });

            removeEnvironmentPromotionPreviewQueries(queryClient, {resourceType, sourceEnvironmentId, sourceId});

            return promoteToEnvironment;
        } catch (error) {
            if (isPromotionPreviewStaleError(error)) {
                setPreviewStale(true);
                setReturnToReviewPending(true);

                void previewQueryResult.refetch();
            }

            throw error;
        }
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

    useEffect(
        () => () =>
            removeEnvironmentPromotionPreviewQueries(queryClient, {resourceType, sourceEnvironmentId, sourceId}),
        [queryClient, resourceType, sourceEnvironmentId, sourceId]
    );

    useEffect(() => {
        if (previousWizardResetKeyRef.current === wizardResetKey) {
            return;
        }

        previousWizardResetKeyRef.current = wizardResetKey;

        setMappings(suggestedMappings);
    }, [suggestedMappings, wizardResetKey]);

    return {
        completeReturnToReview,
        destructiveChangeCount,
        handlePromote,
        handleTargetConnectionIdChange,
        handleTargetEnvironmentIdChange,
        isDestructiveAcknowledged,
        isPreviewError: previewQueryResult.isError,
        isPreviewFetching: previewQueryResult.isFetching,
        isPreviewLoading: previewQueryResult.isLoading,
        isPreviewStale,
        isPromotePending: promoteMutation.isPending,
        isReturnToReviewPending,
        mappings,
        preview,
        previewError: previewQueryResult.error,
        setDestructiveAcknowledged,
        targetEnvironmentId,
        targetEnvironmentLabel,
        targetEnvironmentOptions,
        unresolvedConnectionCount,
        wizardResetKey,
    };
}
