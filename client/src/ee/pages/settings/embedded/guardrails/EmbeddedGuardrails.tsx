import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {Textarea} from '@/components/ui/textarea';
import {
    AiGuardrailsBlockingMode,
    AiGuardrailsSettingsScope,
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
    redactMcpResults: boolean;
    redactPii: boolean;
    redactSecrets: boolean;
    restoreIntoWorkflowOutput: boolean;
    scanResponses: boolean;
}

const DEFAULT_FORM: GuardrailsFormI = {
    blockedTerms: '',
    blockingMode: AiGuardrailsBlockingMode.Block,
    injectionDetectionEnabled: false,
    moderationEnabled: false,
    redactMcpResults: false,
    redactPii: false,
    redactSecrets: false,
    restoreIntoWorkflowOutput: false,
    scanResponses: false,
};

const EmbeddedGuardrails = () => {
    const [form, setForm] = useState<GuardrailsFormI>(DEFAULT_FORM);

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useAiGuardrailsWorkspaceSettingsQuery({
        scope: AiGuardrailsSettingsScope.Embedded,
    });

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
            redactMcpResults: settings?.redactMcpResults ?? false,
            redactPii: settings?.redactPii ?? false,
            redactSecrets: settings?.redactSecrets ?? false,
            restoreIntoWorkflowOutput: settings?.restoreIntoWorkflowOutput ?? false,
            scanResponses: settings?.scanResponses ?? false,
        });
    }, [data]);

    const handleSave = () => {
        updateMutation.mutate({
            input: {
                blockedTerms: blockedTermsList.length > 0 ? blockedTermsList.join(',') : undefined,
                blockingMode: form.blockingMode,
                injectionDetectionEnabled: form.injectionDetectionEnabled,
                minConfidence: data?.aiGuardrailsWorkspaceSettings?.minConfidence ?? undefined,
                moderationEnabled: form.moderationEnabled,
                redactMcpResults: form.redactMcpResults,
                redactPii: form.redactPii,
                redactSecrets: form.redactSecrets,
                restoreIntoWorkflowOutput: form.restoreIntoWorkflowOutput,
                scanResponses: form.scanResponses,
                scope: AiGuardrailsSettingsScope.Embedded,
            },
        });
    };

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
                <div className="space-y-6 py-6">
                    <p className="text-sm text-muted-foreground">
                        Embedded guardrails apply to canvas AI Agent runs in embedded integrations. This single
                        tenant-wide policy governs every connected user -- there is no per-integration or
                        per-connected-user override.
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
                                    Redact whichever of PII and secrets the Redact PII and Redact secrets switches above
                                    enable, from model output before returning it. With both of those off, this does
                                    nothing. Non-streaming completions, and streamed ones when streaming response
                                    scanning is enabled for the deployment.
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
                                <Label htmlFor="restore-into-workflow-output">Restore PII in workflow output</Label>

                                <p className="text-xs text-muted-foreground">
                                    Off (default), downstream workflow nodes and the tools an AI Agent node calls
                                    receive placeholders such as [PII_EMAIL_ADDRESS_1_…] instead of the caller's real
                                    values -- the agent itself still sees real values. Turn this on to hand real values
                                    downstream instead, e.g. so a Slack post or HTTP call wired after the agent gets the
                                    caller's actual address. Streamed replies to a live caller are never affected.
                                </p>
                            </div>

                            <Switch
                                checked={form.restoreIntoWorkflowOutput}
                                id="restore-into-workflow-output"
                                onCheckedChange={(checked) => setForm({...form, restoreIntoWorkflowOutput: checked})}
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

export default EmbeddedGuardrails;
