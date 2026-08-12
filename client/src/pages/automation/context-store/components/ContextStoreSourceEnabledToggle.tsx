import Switch from '@/components/Switch/Switch';
import {ContextStoreSource, useSetContextStoreSourceEnabledMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {toast} from 'sonner';

interface ContextStoreSourceEnabledTogglePropsI {
    isAdmin: boolean;
    source: ContextStoreSource;
}

/**
 * Enabling is a state the row should show, not an action buried in a menu: whether a source syncs on its cadence is
 * the thing an operator scans the table for. Delete stays in the menu — it is not a state, and it is destructive.
 */
const ContextStoreSourceEnabledToggle = ({isAdmin, source}: ContextStoreSourceEnabledTogglePropsI) => {
    const queryClient = useQueryClient();

    const setEnabledMutation = useSetContextStoreSourceEnabledMutation({
        // Enabling schedules the source's trigger, so a malformed cadence surfaces here rather than at sync time.
        // Without this the switch silently sprang back to its old position.
        onError: (mutationError: unknown) => {
            const message = mutationError instanceof Error ? mutationError.message : 'Failed to change enabled state';

            toast.error(message);
        },
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['contextStoreSources']}),
    });

    if (!isAdmin) {
        return null;
    }

    return (
        <Switch
            aria-label={`${source.enabled ? 'Disable' : 'Enable'} ${source.name}`}
            checked={source.enabled}
            onCheckedChange={(enabled) => setEnabledMutation.mutate({enabled, id: source.id})}
        />
    );
};

export default ContextStoreSourceEnabledToggle;
