import WorkflowErrorHandlingDialog from '@/pages/automation/project/components/project-header/components/settings-menu/components/WorkflowErrorHandlingDialog';
import * as graphql from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, test, vi} from 'vitest';

describe('WorkflowErrorHandlingDialog', () => {
    test('disabling clears the override and sets errorWorkflowDisabled', async () => {
        const mutateMock = vi.fn();

        vi.spyOn(graphql, 'useEligibleErrorWorkflowsQuery').mockReturnValue({
            data: {eligibleErrorWorkflows: []},
        } as ReturnType<typeof graphql.useEligibleErrorWorkflowsQuery>);
        vi.spyOn(graphql, 'useProjectWorkflowErrorConfigQuery').mockReturnValue({
            data: {projectWorkflow: {errorProjectWorkflowId: null, errorWorkflowDisabled: false}},
        } as ReturnType<typeof graphql.useProjectWorkflowErrorConfigQuery>);
        vi.spyOn(graphql, 'useUpdateProjectWorkflowErrorWorkflowMutation').mockReturnValue({
            mutate: mutateMock,
        } as unknown as ReturnType<typeof graphql.useUpdateProjectWorkflowErrorWorkflowMutation>);

        const queryClient = new QueryClient();

        render(
            <QueryClientProvider client={queryClient}>
                <WorkflowErrorHandlingDialog
                    onClose={vi.fn()}
                    projectId="1"
                    projectVersion={1}
                    projectWorkflowId="10"
                />
            </QueryClientProvider>
        );

        fireEvent.click(screen.getByLabelText(/disabled/i));
        fireEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith(
                expect.objectContaining({
                    errorProjectWorkflowId: undefined,
                    errorWorkflowDisabled: true,
                    projectWorkflowId: '10',
                })
            )
        );
    });

    test('overriding selects an eligible workflow, excluding the workflow being configured', async () => {
        const mutateMock = vi.fn();

        vi.spyOn(graphql, 'useEligibleErrorWorkflowsQuery').mockReturnValue({
            data: {
                eligibleErrorWorkflows: [
                    {id: '10', workflow: {label: 'Self'}, workflowId: 'wf-10'},
                    {id: '99', workflow: {label: 'Handle Failures'}, workflowId: 'wf-99'},
                ],
            },
        } as ReturnType<typeof graphql.useEligibleErrorWorkflowsQuery>);
        vi.spyOn(graphql, 'useProjectWorkflowErrorConfigQuery').mockReturnValue({
            data: {projectWorkflow: {errorProjectWorkflowId: null, errorWorkflowDisabled: false}},
        } as ReturnType<typeof graphql.useProjectWorkflowErrorConfigQuery>);
        vi.spyOn(graphql, 'useUpdateProjectWorkflowErrorWorkflowMutation').mockReturnValue({
            mutate: mutateMock,
        } as unknown as ReturnType<typeof graphql.useUpdateProjectWorkflowErrorWorkflowMutation>);

        const queryClient = new QueryClient();

        render(
            <QueryClientProvider client={queryClient}>
                <WorkflowErrorHandlingDialog
                    onClose={vi.fn()}
                    projectId="1"
                    projectVersion={1}
                    projectWorkflowId="10"
                />
            </QueryClientProvider>
        );

        fireEvent.click(screen.getByLabelText(/override/i));

        expect(screen.queryByRole('option', {name: 'Self'})).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('combobox'));
        fireEvent.click(await screen.findByRole('option', {name: 'Handle Failures'}));
        fireEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith(
                expect.objectContaining({
                    errorProjectWorkflowId: '99',
                    errorWorkflowDisabled: false,
                    projectWorkflowId: '10',
                })
            )
        );
    });

    test('inherit is the default mode when no override or disable is set', () => {
        vi.spyOn(graphql, 'useEligibleErrorWorkflowsQuery').mockReturnValue({
            data: {eligibleErrorWorkflows: []},
        } as ReturnType<typeof graphql.useEligibleErrorWorkflowsQuery>);
        vi.spyOn(graphql, 'useProjectWorkflowErrorConfigQuery').mockReturnValue({
            data: {projectWorkflow: {errorProjectWorkflowId: null, errorWorkflowDisabled: false}},
        } as ReturnType<typeof graphql.useProjectWorkflowErrorConfigQuery>);
        vi.spyOn(graphql, 'useUpdateProjectWorkflowErrorWorkflowMutation').mockReturnValue({
            mutate: vi.fn(),
        } as unknown as ReturnType<typeof graphql.useUpdateProjectWorkflowErrorWorkflowMutation>);

        const queryClient = new QueryClient();

        render(
            <QueryClientProvider client={queryClient}>
                <WorkflowErrorHandlingDialog
                    onClose={vi.fn()}
                    projectId="1"
                    projectVersion={1}
                    projectWorkflowId="10"
                />
            </QueryClientProvider>
        );

        expect(screen.getByLabelText(/inherit project default/i)).toBeChecked();
    });
});
