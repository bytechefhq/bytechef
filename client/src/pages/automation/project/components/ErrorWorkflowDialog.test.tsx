import ErrorWorkflowDialog from '@/pages/automation/project/components/ErrorWorkflowDialog';
import * as graphql from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, test, vi} from 'vitest';

describe('ErrorWorkflowDialog', () => {
    test('shows the empty-state hint when the project has no eligible handlers', () => {
        vi.spyOn(graphql, 'useEligibleErrorWorkflowsQuery').mockReturnValue({
            data: {eligibleErrorWorkflows: []},
        } as ReturnType<typeof graphql.useEligibleErrorWorkflowsQuery>);
        vi.spyOn(graphql, 'useProjectErrorWorkflowQuery').mockReturnValue({
            data: {project: {errorProjectWorkflowId: null}},
        } as ReturnType<typeof graphql.useProjectErrorWorkflowQuery>);

        const queryClient = new QueryClient();

        render(
            <QueryClientProvider client={queryClient}>
                <ErrorWorkflowDialog onClose={vi.fn()} projectId="1" projectVersion={1} />
            </QueryClientProvider>
        );

        expect(screen.getByText(/add a new workflow error trigger/i)).toBeInTheDocument();
    });

    test('submits the selected handler', async () => {
        const mutateMock = vi.fn();

        vi.spyOn(graphql, 'useEligibleErrorWorkflowsQuery').mockReturnValue({
            data: {
                eligibleErrorWorkflows: [{id: '99', workflow: {label: 'Handle Failures'}, workflowId: 'wf-99'}],
            },
        } as ReturnType<typeof graphql.useEligibleErrorWorkflowsQuery>);
        vi.spyOn(graphql, 'useProjectErrorWorkflowQuery').mockReturnValue({
            data: {project: {errorProjectWorkflowId: null}},
        } as ReturnType<typeof graphql.useProjectErrorWorkflowQuery>);
        vi.spyOn(graphql, 'useUpdateProjectErrorWorkflowMutation').mockReturnValue({
            mutate: mutateMock,
        } as unknown as ReturnType<typeof graphql.useUpdateProjectErrorWorkflowMutation>);

        const queryClient = new QueryClient();

        render(
            <QueryClientProvider client={queryClient}>
                <ErrorWorkflowDialog onClose={vi.fn()} projectId="1" projectVersion={1} />
            </QueryClientProvider>
        );

        fireEvent.click(screen.getByRole('combobox'));
        fireEvent.click(await screen.findByRole('option', {name: 'Handle Failures'}));
        fireEvent.click(screen.getByRole('button', {name: /save/i}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith(
                expect.objectContaining({errorProjectWorkflowId: '99', projectId: '1'})
            )
        );
    });
});
