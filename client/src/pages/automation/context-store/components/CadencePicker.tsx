import {Input} from '@/components/ui/input';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface CadencePickerPropsI {
    onChange: (cadence: string) => void;
    value: string;
}

const PRESETS: Array<{label: string; value: string}> = [
    {label: 'Manual', value: '@manual'},
    {label: 'Hourly', value: '@hourly'},
    {label: 'Daily', value: '@daily'},
    {label: 'Weekly', value: '@weekly'},
];

/**
 * Cadence selector with four common presets ({@code @manual}, {@code @hourly}, {@code @daily}, {@code @weekly}) plus a
 * "Custom" mode that reveals a cron-expression input. The input stays hidden by default to keep the picker focused on
 * the 99% case; advanced users opt in by clicking Custom (or by loading an existing source whose cadence isn't one of
 * the preset values — that case auto-expands the input so the value stays visible/editable).
 */
const CadencePicker = ({onChange, value}: CadencePickerPropsI) => {
    const matchesPreset = PRESETS.some((preset) => preset.value === value);

    // User explicitly opted into custom mode this session. Sticks even when the typed value happens to coincide with
    // a preset, so the input doesn't collapse out from under the cursor mid-edit.
    const [customRequested, setCustomRequested] = useState(false);

    const showCustomInput = !matchesPreset || customRequested;

    return (
        <fieldset className="space-y-2 border-0 p-0">
            <div className="flex flex-wrap gap-2">
                {PRESETS.map((preset) => {
                    const isSelected = !showCustomInput && value === preset.value;

                    return (
                        <button
                            aria-pressed={isSelected}
                            className={twMerge(
                                'rounded-full border px-3 py-1 text-sm',
                                isSelected
                                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                                    : 'border-border bg-background hover:bg-muted'
                            )}
                            key={preset.value}
                            onClick={() => {
                                setCustomRequested(false);
                                onChange(preset.value);
                            }}
                            type="button"
                        >
                            {preset.label}
                        </button>
                    );
                })}

                <button
                    aria-pressed={showCustomInput}
                    className={twMerge(
                        'rounded-full border px-3 py-1 text-sm',
                        showCustomInput
                            ? 'border-blue-500 bg-blue-50 text-blue-700'
                            : 'border-border bg-background hover:bg-muted'
                    )}
                    data-testid="cadence-custom"
                    onClick={() => setCustomRequested(true)}
                    type="button"
                >
                    Custom
                </button>
            </div>

            {showCustomInput && (
                <Input
                    aria-label="Custom cadence"
                    onChange={(event) => onChange(event.target.value)}
                    placeholder='Cron expression (e.g., "0 */15 * * * *")'
                    value={value}
                />
            )}
        </fieldset>
    );
};

export default CadencePicker;
