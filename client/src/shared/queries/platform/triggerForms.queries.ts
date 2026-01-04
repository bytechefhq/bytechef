import {TriggerForm, TriggerFormApi} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const TriggerFormKeys = {
    triggerForm: (id: string) => [...TriggerFormKeys.triggerForms, id],
    triggerForms: ['triggerForms'] as const,
};

export const useGetTriggerFormQuery = (id: string, enabled?: boolean) =>
    useQuery<TriggerForm, Error>({
        enabled: enabled === undefined ? !!id : enabled && !!id,
        queryFn: () => new TriggerFormApi().getTriggerForm({id}),
        queryKey: TriggerFormKeys.triggerForm(id),
    });
