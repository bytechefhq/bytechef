import {ApprovalForm, ApprovalFormApi} from '@/shared/middleware/platform/workflow/execution';
import {useQuery} from '@tanstack/react-query';

export const ApprovalFormKeys = {
    approvalForm: (id: string) => [...ApprovalFormKeys.approvalForms, id],
    approvalForms: ['approvalForms'] as const,
};

export const useGetApprovalFormQuery = (id: string, enabled?: boolean) =>
    useQuery<ApprovalForm, Error>({
        enabled: enabled === undefined ? !!id : enabled && !!id,
        queryFn: () => new ApprovalFormApi().getApprovalForm({id}),
        queryKey: ApprovalFormKeys.approvalForm(id),
        retry: false,
    });
