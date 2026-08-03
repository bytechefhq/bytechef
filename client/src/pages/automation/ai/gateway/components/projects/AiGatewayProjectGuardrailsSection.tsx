import {Checkbox} from '@/components/ui/checkbox';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {useAiGatewayProjectSettingsQuery, useUpdateAiGatewayProjectSettingsMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {forwardRef, useEffect, useImperativeHandle, useMemo, useState} from 'react';

interface GuardrailsFormI {
    blockedTerms: string;
    injectionDetectionEnabled: boolean;
    moderationEnabled: boolean;
    redactPii: boolean;
    redactSecrets: boolean;
    scanResponses: boolean;
}

const EMPTY_FORM: GuardrailsFormI = {
    blockedTerms: '',
    injectionDetectionEnabled: false,
    moderationEnabled: false,
    redactPii: false,
    redactSecrets: false,
    scanResponses: false,
};

const areGuardrailsFormsEqual = (firstForm: GuardrailsFormI, secondForm: GuardrailsFormI): boolean =>
    firstForm.blockedTerms === secondForm.blockedTerms &&
    firstForm.injectionDetectionEnabled === secondForm.injectionDetectionEnabled &&
    firstForm.moderationEnabled === secondForm.moderationEnabled &&
    firstForm.redactPii === secondForm.redactPii &&
    firstForm.redactSecrets === secondForm.redactSecrets &&
    firstForm.scanResponses === secondForm.scanResponses;

export interface AiGatewayProjectGuardrailsSectionHandleI {
    // Resolves true when there was nothing to save or the save succeeded, false on failure.
    // Never rejects, so the dialog can await it unconditionally.
    save: () => Promise<boolean>;
}

interface AiGatewayProjectGuardrailsSectionProps {
    projectId: string;
}

const AiGatewayProjectGuardrailsSection = forwardRef<
    AiGatewayProjectGuardrailsSectionHandleI,
    AiGatewayProjectGuardrailsSectionProps
>(({projectId}, ref) => {
    const [form, setForm] = useState<GuardrailsFormI>(EMPTY_FORM);
    const [savedForm, setSavedForm] = useState<GuardrailsFormI>(EMPTY_FORM);

    const queryClient = useQueryClient();

    const {data} = useAiGatewayProjectSettingsQuery({projectId});

    const updateMutation = useUpdateAiGatewayProjectSettingsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayProjectSettings']});
        },
    });

    const isDirty = useMemo(() => !areGuardrailsFormsEqual(form, savedForm), [form, savedForm]);

    useImperativeHandle(
        ref,
        () => ({
            save: async () => {
                if (!isDirty) {
                    return true;
                }

                try {
                    await updateMutation.mutateAsync({
                        input: {
                            blockedTerms: form.blockedTerms || undefined,
                            injectionDetectionEnabled: form.injectionDetectionEnabled,
                            moderationEnabled: form.moderationEnabled,
                            projectId,
                            redactPii: form.redactPii,
                            redactSecrets: form.redactSecrets,
                            scanResponses: form.scanResponses,
                        },
                    });

                    setSavedForm(form);

                    return true;
                } catch {
                    return false;
                }
            },
        }),
        [form, isDirty, projectId, updateMutation]
    );

    useEffect(() => {
        const settings = data?.aiGatewayProjectSettings;

        if (settings) {
            const loadedForm: GuardrailsFormI = {
                blockedTerms: settings.blockedTerms ?? '',
                injectionDetectionEnabled: settings.injectionDetectionEnabled ?? false,
                moderationEnabled: settings.moderationEnabled ?? false,
                redactPii: settings.redactPii ?? false,
                redactSecrets: settings.redactSecrets ?? false,
                scanResponses: settings.scanResponses ?? false,
            };

            setForm(loadedForm);
            setSavedForm(loadedForm);
        }
    }, [data]);

    return (
        <div className="rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary p-4">
            <fieldset className="space-y-3 border-0">
                <div className="text-sm font-medium">Guardrails</div>

                <p className="text-xs text-content-neutral-secondary">
                    Project overrides layer on top of the workspace guardrails — they can add protection, never remove
                    it. Saved together with the rest of the project when you click Save below.
                </p>

                <div className="flex items-center gap-2">
                    <Checkbox
                        checked={form.redactPii}
                        id="aiGatewayProjectGuardrailsRedactPii"
                        onCheckedChange={(checked) => setForm({...form, redactPii: checked === true})}
                    />

                    <Label htmlFor="aiGatewayProjectGuardrailsRedactPii">
                        Redact PII (emails, SSNs, cards, phones, IPs)
                    </Label>
                </div>

                <div className="flex items-center gap-2">
                    <Checkbox
                        checked={form.redactSecrets}
                        id="aiGatewayProjectGuardrailsRedactSecrets"
                        onCheckedChange={(checked) => setForm({...form, redactSecrets: checked === true})}
                    />

                    <Label htmlFor="aiGatewayProjectGuardrailsRedactSecrets">
                        Redact secrets (API keys, tokens, JWTs, private keys)
                    </Label>
                </div>

                <div className="flex items-center gap-2">
                    <Checkbox
                        checked={form.scanResponses}
                        id="aiGatewayProjectGuardrailsScanResponses"
                        onCheckedChange={(checked) => setForm({...form, scanResponses: checked === true})}
                    />

                    <Label htmlFor="aiGatewayProjectGuardrailsScanResponses">
                        Scan responses (redact PII/secrets from non-streaming output)
                    </Label>
                </div>

                <div className="flex items-center gap-2">
                    <Checkbox
                        checked={form.moderationEnabled}
                        id="aiGatewayProjectGuardrailsModerationEnabled"
                        onCheckedChange={(checked) => setForm({...form, moderationEnabled: checked === true})}
                    />

                    <Label htmlFor="aiGatewayProjectGuardrailsModerationEnabled">
                        Model-based moderation (requires a configured moderation model)
                    </Label>
                </div>

                <div className="flex items-center gap-2">
                    <Checkbox
                        checked={form.injectionDetectionEnabled}
                        id="aiGatewayProjectGuardrailsInjectionDetectionEnabled"
                        onCheckedChange={(checked) => setForm({...form, injectionDetectionEnabled: checked === true})}
                    />

                    <Label htmlFor="aiGatewayProjectGuardrailsInjectionDetectionEnabled">
                        Prompt-injection detection (requires a configured injection model)
                    </Label>
                </div>

                <div className="flex flex-col gap-1">
                    <Label htmlFor="aiGatewayProjectGuardrailsBlockedTerms">
                        Blocked terms (comma-separated; added to the workspace list)
                    </Label>

                    <Input
                        id="aiGatewayProjectGuardrailsBlockedTerms"
                        onChange={(event) => setForm({...form, blockedTerms: event.target.value})}
                        placeholder="none"
                        value={form.blockedTerms}
                    />
                </div>
            </fieldset>
        </div>
    );
});

AiGatewayProjectGuardrailsSection.displayName = 'AiGatewayProjectGuardrailsSection';

export default AiGatewayProjectGuardrailsSection;
