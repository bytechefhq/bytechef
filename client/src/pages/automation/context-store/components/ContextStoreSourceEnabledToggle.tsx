import Switch from '@/components/Switch/Switch';
import {ContextStoreSource, useSetContextStoreSourceEnabledMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {toast} from 'sonner';

interface ContextStoreSourceEnabledTogglePropsI {
    isAdmin: boolean;
    source: ContextStoreSource;
}

const ContextStoreSourceEnabledToggle = ({isAdmin, source}: ContextStoreSourceEnabledTogglePropsI) => {
    const queryClient = useQueryClient();

    const setEnabledMutation = useSetContextStoreSourceEnabledMutation({
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
