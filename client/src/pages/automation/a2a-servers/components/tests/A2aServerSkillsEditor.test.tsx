import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aServerSkillsEditor from '../A2aServerSkillsEditor';

type A2aProjectWorkflowType = {
    id: string;
    skillDescription: string | null;
    skillName: string | null;
    workflowId: string | null;
    workflowLabel: string | null;
};

const hoisted = vi.hoisted(() => ({
    a2aProjectWorkflows: [] as A2aProjectWorkflowType[],
    isPending: false,
    toast: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useA2aProjectWorkflowsByA2aProjectIdQuery: () => ({
        data: {a2aProjectWorkflowsByA2aProjectId: hoisted.a2aProjectWorkflows},
    }),
    useUpdateA2aProjectWorkflowParametersMutation: () => ({
        isPending: hoisted.isPending,
        mutate: hoisted.updateMutate,
    }),
}));

vi.mock('sonner', () => ({toast: hoisted.toast}));

const savedA2aProjectWorkflow: A2aProjectWorkflowType = {
    id: '21',
    skillDescription: 'Answers product questions',
    skillName: 'Product expert',
    workflowId: 'workflow-a',
    workflowLabel: 'Answer question',
};

const unsetA2aProjectWorkflow: A2aProjectWorkflowType = {
    id: '22',
    skillDescription: null,
    skillName: null,
    workflowId: 'workflow-b',
    workflowLabel: 'Create lead',
};

const renderEditor = (queryClient = new QueryClient()) =>
    render(
        <QueryClientProvider client={queryClient}>
            <A2aServerSkillsEditor a2aProjectId="11" />
        </QueryClientProvider>
    );

const getNameInputs = () => screen.getAllByPlaceholderText('Skill name (defaults to the workflow label)');

const getDescriptionInputs = () =>
    screen.getAllByPlaceholderText('Skill description (defaults to the workflow description)');

describe('A2aServerSkillsEditor', () => {
    beforeEach(() => {
        hoisted.a2aProjectWorkflows = [savedA2aProjectWorkflow, unsetA2aProjectWorkflow];
        hoisted.isPending = false;
        hoisted.toast.mockReset();
        hoisted.updateMutate.mockReset();
    });

    it('renders nothing when the A2A project has no workflows', () => {
        hoisted.a2aProjectWorkflows = [];

        const {container} = renderEditor();

        expect(container).toBeEmptyDOMElement();
    });

    it('shows the saved skill values and treats unset values as empty', () => {
        renderEditor();

        expect(screen.getByText('Answer question')).toBeInTheDocument();
        expect(screen.getByText('Create lead')).toBeInTheDocument();

        expect(getNameInputs()[0]).toHaveValue('Product expert');
        expect(getDescriptionInputs()[0]).toHaveValue('Answers product questions');
        expect(getNameInputs()[1]).toHaveValue('');
        expect(getDescriptionInputs()[1]).toHaveValue('');
    });

    it('names each skill input after its workflow', () => {
        renderEditor();

        expect(screen.getByRole('textbox', {name: 'Answer question skill name'})).toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Answer question skill description'})).toBeInTheDocument();
    });

    it('saves the edited skill name and description', async () => {
        const user = userEvent.setup();

        renderEditor();

        await user.type(getNameInputs()[1], 'Lead creator');
        await user.type(getDescriptionInputs()[1], 'Creates CRM leads');
        await user.click(screen.getAllByRole('button', {name: 'Save skill'})[1]);

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.updateMutate.mock.calls[0][0]).toEqual({
            id: '22',
            input: {skillDescription: 'Creates CRM leads', skillName: 'Lead creator'},
        });
    });

    it('refreshes the skills and confirms the save on success', async () => {
        const user = userEvent.setup();
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        hoisted.updateMutate.mockImplementation((_variables, options) => options.onSuccess());

        renderEditor(queryClient);

        await user.click(screen.getAllByRole('button', {name: 'Save skill'})[0]);

        await waitFor(() => expect(hoisted.toast).toHaveBeenCalledWith('The skill has been saved.'));

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['a2aProjectWorkflowsByA2aProjectId']});
    });

    it('does not confirm the save before it succeeds', async () => {
        const user = userEvent.setup();

        renderEditor();

        await user.click(screen.getAllByRole('button', {name: 'Save skill'})[0]);

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.toast).not.toHaveBeenCalled();
    });

    it('disables saving while a save is pending', () => {
        hoisted.isPending = true;

        renderEditor();

        screen.getAllByRole('button', {name: 'Save skill'}).forEach((saveButton) => expect(saveButton).toBeDisabled());
    });
});
