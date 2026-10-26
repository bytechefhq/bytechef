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

const CadencePicker = ({onChange, value}: CadencePickerPropsI) => {
    const matchesPreset = PRESETS.some((preset) => preset.value === value);

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
                                    ? 'border-blue-500 bg-surface-brand-secondary text-content-brand-primary'
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
                            ? 'border-blue-500 bg-surface-brand-secondary text-content-brand-primary'
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
