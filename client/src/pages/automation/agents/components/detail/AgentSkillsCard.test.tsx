import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentSkillsCard from './AgentSkillsCard';

beforeAll(() => {
    // Radix Select relies on pointer-capture APIs that jsdom does not implement.
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
    Element.prototype.scrollIntoView = vi.fn();
});

const {addAgentElementMutate, deleteAgentElementMutate} = vi.hoisted(() => ({
    addAgentElementMutate: vi.fn(),
    deleteAgentElementMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAddAiAgentElementMutation: () => ({isPending: false, mutate: addAgentElementMutate}),
    useAiSkillsQuery: vi.fn(),
    useDeleteAiAgentElementMutation: () => ({isPending: false, mutate: deleteAgentElementMutate}),
}));

const {useAiSkillsQuery} = await import('@/shared/middleware/graphql');

const mockUseAiSkillsQuery = vi.mocked(useAiSkillsQuery);

const SKILL = {
    createdDate: '',
    description: null,
    id: 'skill-1',
    lastModifiedDate: '',
    name: 'Refunds',
    tags: [],
};

const SKILL_ELEMENT = {
    connectionId: null,
    id: 'element-1',
    kind: 'SKILL',
    parameters: {},
    position: 0,
    referenceId: 'skill-1',
};

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

beforeEach(() => {
    addAgentElementMutate.mockReset();
    deleteAgentElementMutate.mockReset();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockUseAiSkillsQuery.mockReset().mockReturnValue({data: {aiSkills: [SKILL]}} as any);
});

describe('AgentSkillsCard', () => {
    it('fires the add-element mutation with kind SKILL and the skill id as referenceId', async () => {
        wrap(<AgentSkillsCard agentId="agent-1" elements={[]} />);

        await userEvent.click(screen.getByRole('button', {name: 'Add skill'}));
        await userEvent.click(screen.getByLabelText('Skill'));
        await userEvent.click(await screen.findByRole('option', {name: 'Refunds'}));
        await userEvent.click(screen.getByRole('button', {name: 'Add'}));

        expect(addAgentElementMutate).toHaveBeenCalledWith({
            input: {agentId: 'agent-1', kind: 'SKILL', referenceId: 'skill-1'},
        });
    });

    it('fires the delete-element mutation when an attached skill is deleted', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        wrap(<AgentSkillsCard agentId="agent-1" elements={[SKILL_ELEMENT] as any} />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete Refunds'}));

        expect(deleteAgentElementMutate).toHaveBeenCalledWith({id: 'element-1'});
    });

    // An attached skill must not be offered again — a second SKILL row for the same skill would carry no
    // extra configuration and so would be a pure duplicate.
    it('offers no skills to add once every skill is already attached', async () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        wrap(<AgentSkillsCard agentId="agent-1" elements={[SKILL_ELEMENT] as any} />);

        await userEvent.click(screen.getByRole('button', {name: 'Add skill'}));

        expect(screen.getByText('No skills left to add')).toBeInTheDocument();
    });

    it('shows the empty state when the agent has no skills attached', () => {
        wrap(<AgentSkillsCard agentId="agent-1" elements={[]} />);

        expect(screen.getByText('No skills added yet.')).toBeInTheDocument();
    });
});
