import AlertDialog from '@/components/AlertDialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {useEffect, useId, useState} from 'react';
import {useFormContext} from 'react-hook-form';

import {
    WEBHOOK_PRESETS,
    WebhookPresetI,
    WebhookPresetSettingsI,
    findWebhookPresetForUrl,
    isWebhookPresetUnchanged,
    isWebhookSettingsEmpty,
} from './webhookPresets';

interface WebhookPresetFormValuesI {
    settings: WebhookPresetSettingsI;
}

const WebhookPresetSelect = () => {
    const [pendingPreset, setPendingPreset] = useState<WebhookPresetI | undefined>();
    const [selectedPreset, setSelectedPreset] = useState<WebhookPresetI | undefined>();

    const presetSelectId = useId();

    const {getValues, setValue} = useFormContext<WebhookPresetFormValuesI>();

    const applyPreset = (preset: WebhookPresetI, shouldDirty: boolean) => {
        setValue('settings.webhook', preset.url, {shouldDirty});
        setValue(
            'settings.webhookHeaders',
            preset.headers.map((header) => ({name: header.name, value: header.value})),
            {shouldDirty}
        );
        setValue('settings.webhookBodyTemplate', preset.template, {shouldDirty});

        setSelectedPreset(preset);
    };

    const handleValueChange = (presetId: string) => {
        const preset = WEBHOOK_PRESETS.find((webhookPreset) => webhookPreset.id === presetId);

        if (!preset) {
            return;
        }

        const settings = getValues('settings');
        const canReplaceSilently =
            isWebhookSettingsEmpty(settings) ||
            (!!selectedPreset && isWebhookPresetUnchanged(settings, selectedPreset));

        if (canReplaceSilently) {
            applyPreset(preset, true);

            return;
        }

        setPendingPreset(preset);
    };

    useEffect(() => {
        const settings = getValues('settings');

        if (isWebhookSettingsEmpty(settings)) {
            applyPreset(WEBHOOK_PRESETS[0], false);
        } else {
            setSelectedPreset(findWebhookPresetForUrl(settings?.webhook));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <>
            <div className="space-y-2">
                <Label htmlFor={presetSelectId}>Preset</Label>

                <Select onValueChange={handleValueChange} value={selectedPreset?.id || ''}>
                    <SelectTrigger className="w-full" id={presetSelectId}>
                        <SelectValue placeholder="Start from a preset" />
                    </SelectTrigger>

                    <SelectContent>
                        {WEBHOOK_PRESETS.map((preset) => (
                            <SelectItem key={preset.id} value={preset.id}>
                                {preset.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>

                <p className="text-sm text-muted-foreground">
                    Fills in the URL, headers and body template. You can edit every field.
                </p>
            </div>

            {pendingPreset && (
                <AlertDialog
                    confirmButtonVariant="default"
                    confirmLabel="Replace"
                    description="The current URL, headers and body template are replaced by the preset."
                    onCancel={() => setPendingPreset(undefined)}
                    onConfirm={() => {
                        applyPreset(pendingPreset, true);

                        setPendingPreset(undefined);
                    }}
                    open
                    title="Replace the URL, headers and body template?"
                />
            )}
        </>
    );
};

export default WebhookPresetSelect;
