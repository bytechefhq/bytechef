import {useSetAiHubPersonalAgentScheduleMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

export const useSetAiHubPersonalAgentSchedule = () => {
    const queryClient = useQueryClient();

    return useSetAiHubPersonalAgentScheduleMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiHubPersonalAgent']}),
    });
};
