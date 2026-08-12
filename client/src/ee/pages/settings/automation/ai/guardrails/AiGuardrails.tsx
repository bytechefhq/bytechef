import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {Switch} from '@/components/ui/switch';
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiGuardrailsBlockingMode,
    useAiGuardrailsWorkspaceSettingsQuery,
    useUpdateAiGuardrailsWorkspaceSettingsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo, useState} from 'react';
import {toast} from 'sonner';

interface GuardrailsFormI {
    blockedTerms: string;
    blockingMode: AiGuardrailsBlockingMode;
    injectionDetectionEnabled: boolean;
    moderationEnabled: boolean;
    redactPii: boolean;
    redactSecrets: boolean;
    scanResponses: boolean;
}

// The query returns null when no settings row exists for the workspace yet -- these are the
// defaults synthesized client-side in that case (every guardrail off, blocking mode BLOCK).
const DEFAULT_FORM: GuardrailsFormI = {
    blockedTerms: '',
    blockingMode: AiGuardrailsBlockingMode.Block,
    injectionDetectionEnabled: false,
    moderationEnabled: false,
    redactPii: false,
    redactSecrets: false,
    scanResponses: false,
};

const AiGuardrails = () => {
    const [form, setForm] = useState<GuardrailsFormI>(DEFAULT_FORM);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useAiGuardrailsWorkspaceSettingsQuery(
        {workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : undefined},
        {enabled: currentWorkspaceId != null}
    );

    const updateMutation = useUpdateAiGuardrailsWorkspaceSettingsMutation({
        onSuccess: () => {
            toast.success('Guardrails saved');

            queryClient.invalidateQueries({queryKey: ['aiGuardrailsWorkspaceSettings']});
        },
    });

    const blockedTermsList = useMemo(
        () =>
            form.blockedTerms
                .split(/[,\n]/)
                .map((term) => term.trim())
                .filter((term) => term.length > 0),
        [form.blockedTerms]
    );

    useEffect(() => {
        const settings = data?.aiGuardrailsWorkspaceSettings;

        setForm({
            blockedTerms: settings?.blockedTerms ?? '',
            blockingMode: settings?.blockingMode ?? AiGuardrailsBlockingMode.Block,
            injectionDetectionEnabled: settings?.injectionDetectionEnabled ?? false,
            moderationEnabled: settings?.moderationEnabled ?? false,
            redactPii: settings?.redactPii ?? false,
            redactSecrets: settings?.redactSecrets ?? false,
            scanResponses: settings?.scanResponses ?? false,
        });
    }, [data]);

    const handleSave = () => {
        if (currentWorkspaceId == null) {
            return;
        }

        updateMutation.mutate({
            input: {
                blockedTerms: blockedTermsList.length > 0 ? blockedTermsList.join(',') : undefined,
                blockingMode: form.blockingMode,
                injectionDetectionEnabled: form.injectionDetectionEnabled,
                moderationEnabled: form.moderationEnabled,
                redactPii: form.redactPii,
                redactSecrets: form.redactSecrets,
                scanResponses: form.scanResponses,
                workspaceId: String(currentWorkspaceId),
            },
        });
    };

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
                <div className="space-y-6 py-6">
                    <p className="text-sm text-muted-foreground">
                        Workspace-level guardrails apply across every AI surface in this workspace: AI Gateway requests,
                        canvas AI Agent runs, and AI Hub chat (personal agents and copilot). Project overlays in AI
                        Gateway settings can add extra protection on top of these, never remove it.
                    </p>

                    <fieldset className="space-y-4 border-0 p-0">
                        <div className="flex items-center justify-between gap-4 rounded-md border p-4">
                            <div className="flex flex-col gap-1">
                                <Label htmlFor="redact-pii">Redact PII</Label>

                                <p className="text-xs text-muted-foreground">
                                    Mask emails, phone numbers, SSNs, cards, and IPs in prompts before they leave
                                    ByteChef; traces store a SHA-256 digest instead of payloads.
                                </p>
                            </div>

                            <Switch
                                checked={form.redactPii}
                                id="redact-pii"
                                onCheckedChange={(checked) => setForm({...form, redactPii: checked})}
                            />
                        </div>

                        <div className="flex items-center justify-between gap-4 rounded-md border p-4">
                            <div className="flex flex-col gap-1">
                                <Label htmlFor="redact-secrets">Redact secrets</Label>

                                <p className="text-xs text-muted-foreground">
                                    Mask API keys, tokens, JWTs, and private keys in prompts before they leave ByteChef.
                                </p>
                            </div>

                            <Switch
                                checked={form.redactSecrets}
                                id="redact-secrets"
                                onCheckedChange={(checked) => setForm({...form, redactSecrets: checked})}
                            />
                        </div>

                        <div className="flex items-center justify-between gap-4 rounded-md border p-4">
                            <div className="flex flex-col gap-1">
                                <Label htmlFor="scan-responses">Scan responses</Label>

                                <p className="text-xs text-muted-foreground">
                                    Redact PII and secrets from model output before returning it. Non-streaming
                                    completions only.
                                </p>
                            </div>

                            <Switch
                                checked={form.scanResponses}
                                id="scan-responses"
                                onCheckedChange={(checked) => setForm({...form, scanResponses: checked})}
                            />
                        </div>

                        <div className="flex items-center justify-between gap-4 rounded-md border p-4">
                            <div className="flex flex-col gap-1">
                                <Label htmlFor="moderation-enabled">Model-based moderation</Label>

                                <p className="text-xs text-muted-foreground">
                                    Reject unsafe prompts across AI Gateway requests, canvas AI Agent runs, and AI Hub
                                    chat. Requires a moderation model configured via the
                                    bytechef.ai.gateway.guardrails.moderation-model property.
                                </p>
                            </div>

                            <Switch
                                checked={form.moderationEnabled}
                                id="moderation-enabled"
                                onCheckedChange={(checked) => setForm({...form, moderationEnabled: checked})}
                            />
                        </div>

                        <div className="flex items-center justify-between gap-4 rounded-md border p-4">
                            <div className="flex flex-col gap-1">
                                <Label htmlFor="injection-detection-enabled">Prompt-injection detection</Label>

                                <p className="text-xs text-muted-foreground">
                                    Reject jailbreak and instruction-override attempts. Requires a configured injection
                                    model.
                                </p>
                            </div>

                            <Switch
                                checked={form.injectionDetectionEnabled}
                                id="injection-detection-enabled"
                                onCheckedChange={(checked) => setForm({...form, injectionDetectionEnabled: checked})}
                            />
                        </div>

                        <div className="flex flex-col gap-2 rounded-md border p-4">
                            <Label htmlFor="blocked-terms">Blocked terms</Label>

                            <p className="text-xs text-muted-foreground">
                                Comma or newline separated. Requests containing one of these terms are rejected.
                            </p>

                            <Textarea
                                id="blocked-terms"
                                onChange={(event) => setForm({...form, blockedTerms: event.target.value})}
                                placeholder="none"
                                value={form.blockedTerms}
                            />
                        </div>
                    </fieldset>

                    <fieldset className="space-y-3 border-0 p-0">
                        <legend className="sr-only">Blocking mode</legend>

                        <div className="space-y-3 rounded-md border p-4">
                            <div>
                                <div className="text-sm font-medium">Blocking mode</div>

                                <p className="text-xs text-muted-foreground">
                                    Redaction guardrails (redact PII, redact secrets, scan responses) always continue --
                                    this setting only governs the blocking guardrails: blocked terms, moderation, and
                                    prompt-injection detection.
                                </p>
                            </div>

                            <RadioGroup
                                className="space-y-2"
                                onValueChange={(value) =>
                                    setForm({...form, blockingMode: value as AiGuardrailsBlockingMode})
                                }
                                value={form.blockingMode}
                            >
                                <div className="flex items-center gap-2">
                                    <RadioGroupItem id="blocking-mode-block" value={AiGuardrailsBlockingMode.Block} />

                                    <label htmlFor="blocking-mode-block">Block -- reject the request</label>
                                </div>

                                <div className="flex items-center gap-2">
                                    <RadioGroupItem
                                        id="blocking-mode-redact-and-continue"
                                        value={AiGuardrailsBlockingMode.RedactAndContinue}
                                    />

                                    <label htmlFor="blocking-mode-redact-and-continue">
                                        Redact and continue -- strip the offending content and let the request proceed
                                    </label>
                                </div>
                            </RadioGroup>
                        </div>
                    </fieldset>

                    <div className="flex justify-end">
                        <Button
                            disabled={updateMutation.isPending}
                            label={updateMutation.isPending ? 'Saving...' : 'Save'}
                            onClick={handleSave}
                        />
                    </div>
                </div>
            </div>
        </PageLoader>
    );
};

export default AiGuardrails;
