import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useAiGatewayWorkspaceSettingsQuery,
    useUpdateAiGatewayWorkspaceSettingsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {toast} from 'sonner';

interface SettingsFormI {
    cacheEnabled: boolean;
    cacheTtlSeconds: string;
    logRetentionDays: string;
    redactPii: boolean;
    retryCount: string;
    softBudgetWarningPct: string;
    timeoutMs: string;
}

const EMPTY_FORM: SettingsFormI = {
    cacheEnabled: false,
    cacheTtlSeconds: '',
    logRetentionDays: '',
    redactPii: false,
    retryCount: '',
    softBudgetWarningPct: '',
    timeoutMs: '',
};

function toOptionalInt(value: string): number | undefined {
    if (value === '' || value == null) {
        return undefined;
    }

    const parsed = Number(value);

    return Number.isFinite(parsed) ? parsed : undefined;
}

const AiGatewaySettings = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [form, setForm] = useState<SettingsFormI>(EMPTY_FORM);

    const queryClient = useQueryClient();

    const {data, isLoading} = useAiGatewayWorkspaceSettingsQuery(
        {workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: currentWorkspaceId != null}
    );

    const updateMutation = useUpdateAiGatewayWorkspaceSettingsMutation({
        onError: (error: Error) => toast.error(`Save failed: ${error.message}`),
        onSuccess: () => {
            toast.success('Settings saved');

            queryClient.invalidateQueries({queryKey: ['aiGatewayWorkspaceSettings']});
        },
    });

    useEffect(() => {
        const settings = data?.aiGatewayWorkspaceSettings;

        if (settings) {
            setForm({
                cacheEnabled: settings.cacheEnabled ?? false,
                cacheTtlSeconds: settings.cacheTtlSeconds != null ? String(settings.cacheTtlSeconds) : '',
                logRetentionDays: settings.logRetentionDays != null ? String(settings.logRetentionDays) : '',
                redactPii: settings.redactPii ?? false,
                retryCount: settings.retryCount != null ? String(settings.retryCount) : '',
                softBudgetWarningPct:
                    settings.softBudgetWarningPct != null ? String(settings.softBudgetWarningPct) : '',
                timeoutMs: settings.timeoutMs != null ? String(settings.timeoutMs) : '',
            });
        }
    }, [data]);

    const handleSubmit = () => {
        if (currentWorkspaceId == null) {
            return;
        }

        updateMutation.mutate({
            input: {
                cacheEnabled: form.cacheEnabled,
                cacheTtlSeconds: toOptionalInt(form.cacheTtlSeconds),
                logRetentionDays: toOptionalInt(form.logRetentionDays),
                redactPii: form.redactPii,
                retryCount: toOptionalInt(form.retryCount),
                softBudgetWarningPct: toOptionalInt(form.softBudgetWarningPct),
                timeoutMs: toOptionalInt(form.timeoutMs),
                workspaceId: String(currentWorkspaceId),
            },
        });
    };

    if (isLoading) {
        return <PageLoader loading={true} />;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="py-6">
                <h3 className="text-lg font-medium">Gateway Settings</h3>

                <p className="mt-1 text-sm text-muted-foreground">
                    Workspace-level overrides. Leave a field empty to inherit the system default.
                </p>

                <fieldset className="mt-6 space-y-4 border-0 p-0">
                    <div className="grid grid-cols-2 gap-4">
                        <label className="flex flex-col gap-1 text-sm">
                            Retry count
                            <input
                                className="rounded-md border px-3 py-2 text-sm"
                                onChange={(event) => setForm({...form, retryCount: event.target.value})}
                                placeholder="inherit"
                                type="number"
                                value={form.retryCount}
                            />
                        </label>

                        <label className="flex flex-col gap-1 text-sm">
                            Timeout (ms)
                            <input
                                className="rounded-md border px-3 py-2 text-sm"
                                onChange={(event) => setForm({...form, timeoutMs: event.target.value})}
                                placeholder="inherit"
                                type="number"
                                value={form.timeoutMs}
                            />
                        </label>

                        <label className="flex flex-col gap-1 text-sm">
                            Cache TTL (seconds)
                            <input
                                className="rounded-md border px-3 py-2 text-sm"
                                onChange={(event) => setForm({...form, cacheTtlSeconds: event.target.value})}
                                placeholder="inherit"
                                type="number"
                                value={form.cacheTtlSeconds}
                            />
                        </label>

                        <label className="flex flex-col gap-1 text-sm">
                            Log retention (days)
                            <input
                                className="rounded-md border px-3 py-2 text-sm"
                                onChange={(event) => setForm({...form, logRetentionDays: event.target.value})}
                                placeholder="inherit"
                                type="number"
                                value={form.logRetentionDays}
                            />
                        </label>

                        <label className="flex flex-col gap-1 text-sm">
                            Soft budget warning (%)
                            <input
                                className="rounded-md border px-3 py-2 text-sm"
                                max={100}
                                min={0}
                                onChange={(event) => setForm({...form, softBudgetWarningPct: event.target.value})}
                                placeholder="inherit"
                                type="number"
                                value={form.softBudgetWarningPct}
                            />
                        </label>
                    </div>

                    <label className="flex items-center gap-2 text-sm">
                        <input
                            checked={form.cacheEnabled}
                            onChange={(event) => setForm({...form, cacheEnabled: event.target.checked})}
                            type="checkbox"
                        />
                        Response caching enabled
                    </label>

                    <label className="flex items-center gap-2 text-sm">
                        <input
                            checked={form.redactPii}
                            onChange={(event) => setForm({...form, redactPii: event.target.checked})}
                            type="checkbox"
                        />
                        Redact PII (store SHA-256 digest instead of input/output payloads)
                    </label>
                </fieldset>

                <div className="mt-6 flex justify-end">
                    <Button
                        disabled={updateMutation.isPending}
                        label={updateMutation.isPending ? 'Saving...' : 'Save'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiGatewaySettings;
