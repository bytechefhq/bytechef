import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiGatewayRateLimit,
    AiGatewayRateLimitScope,
    AiGatewayRateLimitType,
    useAiGatewayRateLimitsQuery,
    useCreateAiGatewayRateLimitMutation,
    useDeleteAiGatewayRateLimitMutation,
    useUpdateAiGatewayRateLimitMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, PlusIcon, ShieldIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const SCOPE_OPTIONS: AiGatewayRateLimitScope[] = [
    AiGatewayRateLimitScope.Global,
    AiGatewayRateLimitScope.PerUser,
    AiGatewayRateLimitScope.PerProperty,
];

const LIMIT_TYPE_OPTIONS: AiGatewayRateLimitType[] = [
    AiGatewayRateLimitType.Requests,
    AiGatewayRateLimitType.Tokens,
    AiGatewayRateLimitType.Cost,
];

const formatScope = (scope: AiGatewayRateLimitScope): string => {
    switch (scope) {
        case AiGatewayRateLimitScope.Global:
            return 'Global';
        case AiGatewayRateLimitScope.PerUser:
            return 'Per User';
        case AiGatewayRateLimitScope.PerProperty:
            return 'Per Property';
        default:
            return scope;
    }
};

const formatLimitType = (limitType: AiGatewayRateLimitType): string => {
    switch (limitType) {
        case AiGatewayRateLimitType.Requests:
            return 'Requests';
        case AiGatewayRateLimitType.Tokens:
            return 'Tokens';
        case AiGatewayRateLimitType.Cost:
            return 'Cost';
        default:
            return limitType;
    }
};

const formatWindowSeconds = (seconds: number): string => {
    if (seconds < 60) {
        return `${seconds}s`;
    }

    if (seconds < 3600) {
        return `${Math.floor(seconds / 60)}m`;
    }

    if (seconds < 86400) {
        return `${Math.floor(seconds / 3600)}h`;
    }

    return `${Math.floor(seconds / 86400)}d`;
};

// Rate limit analytics (future work): a "View History" view per rate limit that shows the last 24h count of 429
// rejections attributed to this rule is not yet implemented. Rate limit counters are currently stored in Redis/memory
// only and hit history is not persisted. Landing this requires either persisting a new ai_gateway_rate_limit_hit
// table or backfilling from request-log traces with status=429 and a matching rate-limit attribution tag — both of
// which are too large for this pass.
const AiGatewayRateLimits = () => {
    const [confirmingDeleteId, setConfirmingDeleteId] = useState<string | undefined>(undefined);
    const [editingRateLimit, setEditingRateLimit] = useState<AiGatewayRateLimit | undefined>(undefined);
    const [enabled, setEnabled] = useState(true);
    const [limitType, setLimitType] = useState<AiGatewayRateLimitType>(AiGatewayRateLimitType.Requests);
    const [limitValue, setLimitValue] = useState('100');
    const [name, setName] = useState('');
    const [propertyKey, setPropertyKey] = useState('');
    const [scope, setScope] = useState<AiGatewayRateLimitScope>(AiGatewayRateLimitScope.Global);
    const [showForm, setShowForm] = useState(false);
    const [windowSeconds, setWindowSeconds] = useState('60');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: rateLimitsData, isLoading} = useAiGatewayRateLimitsQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const rateLimits = rateLimitsData?.aiGatewayRateLimits || [];

    const createMutation = useCreateAiGatewayRateLimitMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayRateLimits']});

            resetForm();
        },
    });

    const updateMutation = useUpdateAiGatewayRateLimitMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayRateLimits']});

            resetForm();
        },
    });

    const deleteMutation = useDeleteAiGatewayRateLimitMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayRateLimits']});

            setConfirmingDeleteId(undefined);
        },
    });

    const resetForm = useCallback(() => {
        setEditingRateLimit(undefined);
        setEnabled(true);
        setLimitType(AiGatewayRateLimitType.Requests);
        setLimitValue('100');
        setName('');
        setPropertyKey('');
        setScope(AiGatewayRateLimitScope.Global);
        setShowForm(false);
        setWindowSeconds('60');
    }, []);

    const handleEdit = useCallback((rateLimit: AiGatewayRateLimit) => {
        setEditingRateLimit(rateLimit);
        setEnabled(rateLimit.enabled);
        setLimitType(rateLimit.limitType);
        setLimitValue(rateLimit.limitValue.toString());
        setName(rateLimit.name);
        setPropertyKey(rateLimit.propertyKey || '');
        setScope(rateLimit.scope);
        setShowForm(true);
        setWindowSeconds(rateLimit.windowSeconds.toString());
    }, []);

    const handleConfirmDelete = useCallback(() => {
        if (confirmingDeleteId) {
            deleteMutation.mutate({id: confirmingDeleteId});
        }
    }, [confirmingDeleteId, deleteMutation]);

    const handleSubmit = useCallback(() => {
        if (editingRateLimit) {
            updateMutation.mutate({
                id: editingRateLimit.id,
                input: {
                    enabled,
                    limitType,
                    limitValue: parseInt(limitValue, 10),
                    name: name || undefined,
                    propertyKey: scope === AiGatewayRateLimitScope.PerProperty ? propertyKey : undefined,
                    scope,
                    windowSeconds: parseInt(windowSeconds, 10),
                },
            });
        } else {
            createMutation.mutate({
                input: {
                    enabled,
                    limitType,
                    limitValue: parseInt(limitValue, 10),
                    name,
                    propertyKey: scope === AiGatewayRateLimitScope.PerProperty ? propertyKey : undefined,
                    scope,
                    windowSeconds: parseInt(windowSeconds, 10),
                    workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
                },
            });
        }
    }, [
        createMutation,
        currentWorkspaceId,
        editingRateLimit,
        enabled,
        limitType,
        limitValue,
        name,
        propertyKey,
        scope,
        updateMutation,
        windowSeconds,
    ]);

    if (isLoading) {
        return <PageLoader loading={true} />;
    }

    if (showForm) {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <div className="py-4">
                    <h3 className="text-lg font-medium">
                        {editingRateLimit ? 'Edit Rate Limit' : 'Create Rate Limit'}
                    </h3>
                </div>

                <div className="max-w-md space-y-4">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="e.g., Global request limit"
                            type="text"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Scope</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setScope(event.target.value as AiGatewayRateLimitScope)}
                            value={scope}
                        >
                            {SCOPE_OPTIONS.map((scopeOption) => (
                                <option key={scopeOption} value={scopeOption}>
                                    {formatScope(scopeOption)}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    {scope === AiGatewayRateLimitScope.PerProperty && (
                        <fieldset className="border-0">
                            <label className="mb-1 block text-sm font-medium">Property Key</label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                onChange={(event) => setPropertyKey(event.target.value)}
                                placeholder="e.g., customer_id"
                                type="text"
                                value={propertyKey}
                            />

                            <p className="mt-1 text-xs text-muted-foreground">
                                The custom property key to group rate limits by.
                            </p>
                        </fieldset>
                    )}

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Limit Type</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setLimitType(event.target.value as AiGatewayRateLimitType)}
                            value={limitType}
                        >
                            {LIMIT_TYPE_OPTIONS.map((typeOption) => (
                                <option key={typeOption} value={typeOption}>
                                    {formatLimitType(typeOption)}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Limit Value</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            min="1"
                            onChange={(event) => setLimitValue(event.target.value)}
                            placeholder="100"
                            type="number"
                            value={limitValue}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Window (seconds)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            min="1"
                            onChange={(event) => setWindowSeconds(event.target.value)}
                            placeholder="60"
                            type="number"
                            value={windowSeconds}
                        />

                        <p className="mt-1 text-xs text-muted-foreground">Time window in seconds for the rate limit.</p>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="flex items-center gap-2 text-sm font-medium">
                            <input
                                checked={enabled}
                                onChange={(event) => setEnabled(event.target.checked)}
                                type="checkbox"
                            />
                            Enabled
                        </label>
                    </fieldset>

                    <div className="flex gap-2 pt-2">
                        <Button label="Cancel" onClick={resetForm} variant="outline" />

                        <Button
                            disabled={!name || !limitValue || createMutation.isPending || updateMutation.isPending}
                            label={editingRateLimit ? 'Save' : 'Create'}
                            onClick={handleSubmit}
                        />
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {rateLimits.length === 0 ? (
                <EmptyList
                    button={<Button label="Create Rate Limit" onClick={() => setShowForm(true)} />}
                    icon={<ShieldIcon className="size-12 text-muted-foreground" />}
                    message="Set up rate limits to control LLM gateway usage."
                    title="No Rate Limits Configured"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end py-4">
                        <Button
                            icon={<PlusIcon className="size-4" />}
                            label="New Rate Limit"
                            onClick={() => setShowForm(true)}
                        />
                    </div>

                    <div className="overflow-x-auto rounded-lg border">
                        <table className="w-full text-left text-sm">
                            <thead className="border-b bg-muted/50">
                                <tr>
                                    <th className="px-4 py-3 font-medium">Name</th>

                                    <th className="px-4 py-3 font-medium">Scope</th>

                                    <th className="px-4 py-3 font-medium">Limit Type</th>

                                    <th className="px-4 py-3 font-medium">Value</th>

                                    <th className="px-4 py-3 font-medium">Window</th>

                                    <th className="px-4 py-3 font-medium">Enabled</th>

                                    <th className="px-4 py-3 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {rateLimits.map((rateLimit) => (
                                    <tr className="border-b last:border-b-0" key={rateLimit.id}>
                                        <td className="px-4 py-3 font-medium">{rateLimit.name}</td>

                                        <td className="px-4 py-3">
                                            {formatScope(rateLimit.scope)}

                                            {rateLimit.scope === AiGatewayRateLimitScope.PerProperty &&
                                                rateLimit.propertyKey && (
                                                    <span className="ml-1 text-xs text-muted-foreground">
                                                        ({rateLimit.propertyKey})
                                                    </span>
                                                )}
                                        </td>

                                        <td className="px-4 py-3">{formatLimitType(rateLimit.limitType)}</td>

                                        <td className="px-4 py-3">{rateLimit.limitValue}</td>

                                        <td className="px-4 py-3">{formatWindowSeconds(rateLimit.windowSeconds)}</td>

                                        <td className="px-4 py-3">
                                            <span
                                                className={twMerge(
                                                    'rounded-full px-2 py-0.5 text-xs font-medium',
                                                    rateLimit.enabled
                                                        ? 'bg-green-100 text-green-800'
                                                        : 'bg-gray-100 text-gray-800'
                                                )}
                                            >
                                                {rateLimit.enabled ? 'Active' : 'Disabled'}
                                            </span>
                                        </td>

                                        <td className="px-4 py-3">
                                            <div className="flex gap-1">
                                                <button
                                                    className="rounded p-1 hover:bg-muted"
                                                    onClick={() => handleEdit(rateLimit)}
                                                    title="Edit"
                                                >
                                                    <PencilIcon className="size-4" />
                                                </button>

                                                <button
                                                    className="rounded p-1 hover:bg-muted"
                                                    onClick={() => setConfirmingDeleteId(rateLimit.id)}
                                                    title="Delete"
                                                >
                                                    <TrashIcon className="size-4" />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            <DeleteAlertDialog
                onCancel={() => setConfirmingDeleteId(undefined)}
                onDelete={handleConfirmDelete}
                open={!!confirmingDeleteId}
            />
        </div>
    );
};

export default AiGatewayRateLimits;
