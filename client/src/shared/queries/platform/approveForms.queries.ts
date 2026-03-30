import {ApproveForm, ApproveFormApi} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ApproveFormKeys = {
    approveForm: (id: string) => [...ApproveFormKeys.approveForms, id],
    approveForms: ['approveForms'] as const,
};

export const useGetApproveFormQuery = (id: string, enabled?: boolean) =>
    useQuery<ApproveForm, Error>({
        enabled: enabled === undefined ? !!id : enabled && !!id,
        queryFn: () => new ApproveFormApi().getApproveForm({id}),
        queryKey: ApproveFormKeys.approveForm(id),
    });
