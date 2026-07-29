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
    blockedTerms: string;
    cacheEnabled: boolean;
    cacheTtlSeconds: string;
    injectionDetectionEnabled: boolean;
    logRetentionDays: string;
    moderationEnabled: boolean;
    redactPii: boolean;
    redactSecrets: boolean;
    retryCount: string;
    scanResponses: boolean;
    softBudgetWarningPct: string;
    timeoutMs: string;
}

const EMPTY_FORM: SettingsFormI = {
    blockedTerms: '',
    cacheEnabled: false,
    cacheTtlSeconds: '',
    injectionDetectionEnabled: false,
    logRetentionDays: '',
    moderationEnabled: false,
    redactPii: false,
    redactSecrets: false,
    retryCount: '',
    scanResponses: false,
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
                blockedTerms: settings.blockedTerms ?? '',
                cacheEnabled: settings.cacheEnabled ?? false,
                cacheTtlSeconds: settings.cacheTtlSeconds != null ? String(settings.cacheTtlSeconds) : '',
                injectionDetectionEnabled: settings.injectionDetectionEnabled ?? false,
                logRetentionDays: settings.logRetentionDays != null ? String(settings.logRetentionDays) : '',
                moderationEnabled: settings.moderationEnabled ?? false,
                redactPii: settings.redactPii ?? false,
                redactSecrets: settings.redactSecrets ?? false,
                retryCount: settings.retryCount != null ? String(settings.retryCount) : '',
                scanResponses: settings.scanResponses ?? false,
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
                blockedTerms: form.blockedTerms || undefined,
                cacheEnabled: form.cacheEnabled,
                cacheTtlSeconds: toOptionalInt(form.cacheTtlSeconds),
                injectionDetectionEnabled: form.injectionDetectionEnabled,
                logRetentionDays: toOptionalInt(form.logRetentionDays),
                moderationEnabled: form.moderationEnabled,
                redactPii: form.redactPii,
                redactSecrets: form.redactSecrets,
                retryCount: toOptionalInt(form.retryCount),
                scanResponses: form.scanResponses,
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
                        Redact PII (mask emails, phone numbers, SSNs, cards, and IPs in prompts before they leave
                        ByteChef; traces store a SHA-256 digest instead of payloads)
                    </label>

                    <label className="flex items-center gap-2 text-sm">
                        <input
                            checked={form.redactSecrets}
                            onChange={(event) => setForm({...form, redactSecrets: event.target.checked})}
                            type="checkbox"
                        />
                        Redact secrets (mask API keys, tokens, JWTs, and private keys in prompts before they leave
                        ByteChef)
                    </label>

                    <label className="flex items-center gap-2 text-sm">
                        <input
                            checked={form.scanResponses}
                            onChange={(event) => setForm({...form, scanResponses: event.target.checked})}
                            type="checkbox"
                        />
                        Scan responses (redact PII and secrets from model output before returning it; non-streaming
                        completions only)
                    </label>

                    <label className="flex items-center gap-2 text-sm">
                        <input
                            checked={form.moderationEnabled}
                            onChange={(event) => setForm({...form, moderationEnabled: event.target.checked})}
                            type="checkbox"
                        />
                        Model-based moderation (reject unsafe prompts; requires a configured moderation model)
                    </label>

                    <label className="flex items-center gap-2 text-sm">
                        <input
                            checked={form.injectionDetectionEnabled}
                            onChange={(event) => setForm({...form, injectionDetectionEnabled: event.target.checked})}
                            type="checkbox"
                        />
                        Prompt-injection detection (reject jailbreak / instruction-override attempts; requires a
                        configured injection model)
                    </label>

                    <label className="flex flex-col gap-1 text-sm">
                        Blocked terms (comma-separated; requests containing one are rejected)
                        <input
                            className="rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setForm({...form, blockedTerms: event.target.value})}
                            placeholder="none"
                            value={form.blockedTerms}
                        />
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
