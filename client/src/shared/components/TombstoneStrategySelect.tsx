import {Label} from '@/components/ui/label';

export const TOMBSTONE_STRATEGY_VALUES = {
    NONE: 'NONE',
    PERIODIC_FULL_REPLACE: 'PERIODIC_FULL_REPLACE',
    UPSTREAM_CHANGE_FEED: 'UPSTREAM_CHANGE_FEED',
} as const;

interface TombstoneStrategySelectProps {
    onChange: (value: string) => void;
    helpText?: string;
    testId?: string;
    value: string;
}

const TombstoneStrategySelect = ({helpText, onChange, testId, value}: TombstoneStrategySelectProps) => {
    const defaultHelpText =
        'How the source detects upstream deletions. Defaults to a periodic full re-sync (recommended). Change only ' +
        'when the source emits its own deletion events.';

    return (
        <fieldset className="space-y-2 border-0 p-0">
            <Label>Tombstone Strategy</Label>

            <p className="text-xs text-muted-foreground">{helpText ?? defaultHelpText}</p>

            <select
                className="w-full rounded-md border border-border bg-background px-2 py-1 text-sm"
                data-testid={testId}
                onChange={(event) => onChange(event.target.value)}
                value={value}
            >
                <option value={TOMBSTONE_STRATEGY_VALUES.PERIODIC_FULL_REPLACE}>
                    Periodic full re-sync (recommended)
                </option>

                <option value={TOMBSTONE_STRATEGY_VALUES.UPSTREAM_CHANGE_FEED}>
                    Upstream change feed (source emits deletes)
                </option>

                <option value={TOMBSTONE_STRATEGY_VALUES.NONE}>None (append-only)</option>
            </select>
        </fieldset>
    );
};

export default TombstoneStrategySelect;
