import Button from '@/components/Button/Button';
import {useAiGatewayProjectSettingsQuery, useUpdateAiGatewayProjectSettingsMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {toast} from 'sonner';

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

interface AiGatewayProjectGuardrailsSectionProps {
    projectId: string;
}

const AiGatewayProjectGuardrailsSection = ({projectId}: AiGatewayProjectGuardrailsSectionProps) => {
    const [form, setForm] = useState<GuardrailsFormI>(EMPTY_FORM);

    const queryClient = useQueryClient();

    const {data} = useAiGatewayProjectSettingsQuery({projectId});

    const updateMutation = useUpdateAiGatewayProjectSettingsMutation({
        onSuccess: () => {
            toast.success('Project guardrails saved');

            queryClient.invalidateQueries({queryKey: ['aiGatewayProjectSettings']});
        },
    });

    useEffect(() => {
        const settings = data?.aiGatewayProjectSettings;

        if (settings) {
            setForm({
                blockedTerms: settings.blockedTerms ?? '',
                injectionDetectionEnabled: settings.injectionDetectionEnabled ?? false,
                moderationEnabled: settings.moderationEnabled ?? false,
                redactPii: settings.redactPii ?? false,
                redactSecrets: settings.redactSecrets ?? false,
                scanResponses: settings.scanResponses ?? false,
            });
        }
    }, [data]);

    const handleSave = () => {
        updateMutation.mutate({
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
    };

    return (
        <fieldset className="space-y-3 border-0">
            <div className="text-sm font-medium">Guardrails</div>

            <p className="text-xs text-muted-foreground">
                Project overrides layer on top of the workspace guardrails — they can add protection, never remove it.
            </p>

            <label className="flex items-center gap-2 text-sm">
                <input
                    checked={form.redactPii}
                    onChange={(event) => setForm({...form, redactPii: event.target.checked})}
                    type="checkbox"
                />
                Redact PII (emails, SSNs, cards, phones, IPs)
            </label>

            <label className="flex items-center gap-2 text-sm">
                <input
                    checked={form.redactSecrets}
                    onChange={(event) => setForm({...form, redactSecrets: event.target.checked})}
                    type="checkbox"
                />
                Redact secrets (API keys, tokens, JWTs, private keys)
            </label>

            <label className="flex items-center gap-2 text-sm">
                <input
                    checked={form.scanResponses}
                    onChange={(event) => setForm({...form, scanResponses: event.target.checked})}
                    type="checkbox"
                />
                Scan responses (redact PII/secrets from non-streaming output)
            </label>

            <label className="flex items-center gap-2 text-sm">
                <input
                    checked={form.moderationEnabled}
                    onChange={(event) => setForm({...form, moderationEnabled: event.target.checked})}
                    type="checkbox"
                />
                Model-based moderation (requires a configured moderation model)
            </label>

            <label className="flex items-center gap-2 text-sm">
                <input
                    checked={form.injectionDetectionEnabled}
                    onChange={(event) => setForm({...form, injectionDetectionEnabled: event.target.checked})}
                    type="checkbox"
                />
                Prompt-injection detection (requires a configured injection model)
            </label>

            <label className="flex flex-col gap-1 text-sm">
                Blocked terms (comma-separated; added to the workspace list)
                <input
                    className="rounded-md border px-3 py-2 text-sm"
                    onChange={(event) => setForm({...form, blockedTerms: event.target.value})}
                    placeholder="none"
                    value={form.blockedTerms}
                />
            </label>

            <div className="flex justify-end">
                <Button
                    disabled={updateMutation.isPending}
                    label={updateMutation.isPending ? 'Saving...' : 'Save guardrails'}
                    onClick={handleSave}
                />
            </div>
        </fieldset>
    );
};

export default AiGatewayProjectGuardrailsSection;
