import {useAgentExternalChangeStore} from '@/pages/automation/agents/stores/useAgentExternalChangeStore';
import refreshAgentAfterExternalChange from '@/pages/automation/agents/utils/refreshAgentAfterExternalChange';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

describe('refreshAgentAfterExternalChange', () => {
    beforeEach(() => {
        useAgentExternalChangeStore.setState({revision: 0});
    });

    it('bumps the revision only after the agent queries were invalidated', async () => {
        const queryClient = new QueryClient();

        queryClient.setQueryData(['aiAgent', {id: 'agent-1'}], {aiAgent: {instructions: 'Be terse.'}});

        const pendingInvalidations: Array<() => void> = [];

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries').mockImplementation(
            () =>
                new Promise<void>((resolve) => {
                    pendingInvalidations.push(resolve);
                })
        );

        const refresh = refreshAgentAfterExternalChange(queryClient);

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['aiAgent']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['aiAgents']});
        expect(useAgentExternalChangeStore.getState().revision).toBe(0);

        queryClient.setQueryData(['aiAgent', {id: 'agent-1'}], {aiAgent: {instructions: 'Be verbose.'}});

        pendingInvalidations.forEach((resolveInvalidation) => resolveInvalidation());

        await refresh;

        expect(useAgentExternalChangeStore.getState().revision).toBe(1);
    });

    it('leaves the revision alone when the refetched agent did not change', async () => {
        const queryClient = new QueryClient();

        queryClient.setQueryData(['aiAgent', {id: 'agent-1'}], {aiAgent: {instructions: 'Be terse.'}});

        vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue();

        await refreshAgentAfterExternalChange(queryClient);

        expect(useAgentExternalChangeStore.getState().revision).toBe(0);
    });
});
