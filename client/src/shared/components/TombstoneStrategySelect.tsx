import {Label} from '@/components/ui/label';

/**
 * Stable wire-level tombstone strategy values. Match {@code com.bytechef.ee.platform.contextstore.domain.TombstoneStrategy}
 * and {@code com.bytechef.platform.knowledgebase.domain.TombstoneStrategy} — the Context Store and Knowledge Base
 * GraphQL enums generate distinct TypeScript types with the same underlying string values, so this component is generic
 * over plain {@code string} to let both callers reuse it.
 */
export const TOMBSTONE_STRATEGY_VALUES = {
    NONE: 'NONE',
    PERIODIC_FULL_REPLACE: 'PERIODIC_FULL_REPLACE',
    UPSTREAM_CHANGE_FEED: 'UPSTREAM_CHANGE_FEED',
} as const;

interface TombstoneStrategySelectProps {
    onChange: (value: string) => void;
    /** Optional override for the help text underneath the label. */
    helpText?: string;
    /** Optional `data-testid` for the underlying select element. */
    testId?: string;
    value: string;
}

/**
 * Tombstone Strategy picker used by both Add Context Source and Add Knowledge Base Source dialogs. Extracted to one
 * place after the help-text drifted from the dropdown label — now both surfaces share the same copy.
 *
 * The component takes a plain string for {@link TombstoneStrategySelectProps.value} because the two callers'
 * generated enums (`ContextStoreTombstoneStrategy`, `TombstoneStrategy`) are distinct nominal types that happen to
 * carry the same wire values. Callers cast at the boundary.
 */
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
