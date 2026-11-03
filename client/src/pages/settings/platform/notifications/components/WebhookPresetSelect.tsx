import AlertDialog from '@/components/AlertDialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {useId, useState} from 'react';
import {useFormContext} from 'react-hook-form';

import {WEBHOOK_PRESETS, WebhookPresetI} from './webhookPresets';

interface WebhookPresetFormValuesI {
    settings: {
        webhook?: string;
        webhookBodyTemplate?: string;
        webhookHeaders?: Array<{name: string; value: string}>;
    };
}

const WebhookPresetSelect = () => {
    const [pendingPreset, setPendingPreset] = useState<WebhookPresetI | undefined>();

    const presetSelectId = useId();

    const {getValues, setValue} = useFormContext<WebhookPresetFormValuesI>();

    const applyPreset = (preset: WebhookPresetI) => {
        setValue('settings.webhook', preset.url, {shouldDirty: true});
        setValue(
            'settings.webhookHeaders',
            preset.headers.map((header) => ({name: header.name, value: header.value})),
            {shouldDirty: true}
        );
        setValue('settings.webhookBodyTemplate', preset.template, {shouldDirty: true});
    };

    const handleValueChange = (presetId: string) => {
        const preset = WEBHOOK_PRESETS.find((webhookPreset) => webhookPreset.id === presetId);

        if (!preset) {
            return;
        }

        const settings = getValues('settings');
        const hasValues = !!settings?.webhook || !!settings?.webhookBodyTemplate || !!settings?.webhookHeaders?.length;

        if (hasValues) {
            setPendingPreset(preset);

            return;
        }

        applyPreset(preset);
    };

    return (
        <>
            <div className="space-y-2">
                <Label htmlFor={presetSelectId}>Preset</Label>

                <Select onValueChange={handleValueChange} value="">
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
                        applyPreset(pendingPreset);

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
