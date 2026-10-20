import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import EmbeddedWorkflowExecutionsDropdownMenu from './EmbeddedWorkflowExecutionsDropdownMenu';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    mutate: vi.fn(),
    stopJobMutationOptions: {} as {onSuccess?: () => void},
    toast: vi.fn(),
}));

vi.mock('@/shared/mutations/platform/jobs.mutations', () => ({
    useStopJobMutation: (options: {onSuccess?: () => void}) => {
        hoisted.stopJobMutationOptions = options;

        return {mutate: hoisted.mutate};
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('sonner', () => ({
    toast: hoisted.toast,
}));

describe('EmbeddedWorkflowExecutionsDropdownMenu', () => {
    beforeEach(() => {
        hoisted.invalidateQueries.mockClear();
        hoisted.mutate.mockClear();
        hoisted.toast.mockClear();
    });

    it('offers View and calls back when it is chosen', async () => {
        const onView = vi.fn();

        render(<EmbeddedWorkflowExecutionsDropdownMenu jobId="7" jobStatus="STARTED" onView={onView} />);

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(await screen.findByText('View'));

        expect(onView).toHaveBeenCalledTimes(1);
    });

    it('stops a started job through the stop mutation', async () => {
        render(<EmbeddedWorkflowExecutionsDropdownMenu jobId="7" jobStatus="STARTED" onView={vi.fn()} />);

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(await screen.findByText('Stop'));

        expect(hoisted.mutate).toHaveBeenCalledWith(7);
    });

    it('disables Stop when the job is not started', async () => {
        render(<EmbeddedWorkflowExecutionsDropdownMenu jobId="7" jobStatus="COMPLETED" onView={vi.fn()} />);

        await userEvent.click(screen.getByRole('button'));

        const stopItem = await screen.findByText('Stop');

        expect(stopItem.closest('[role="menuitem"]')).toHaveAttribute('aria-disabled', 'true');
    });

    it('announces the stop and refreshes both execution lists once the job is stopped', () => {
        render(<EmbeddedWorkflowExecutionsDropdownMenu jobId="7" jobStatus="STARTED" onView={vi.fn()} />);

        hoisted.stopJobMutationOptions.onSuccess?.();

        expect(hoisted.toast).toHaveBeenCalledWith('Stopping Workflow Execution');
        expect(hoisted.invalidateQueries).toHaveBeenCalledTimes(2);
    });
});
