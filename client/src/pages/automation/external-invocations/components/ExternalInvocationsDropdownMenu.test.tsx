import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ExternalInvocationsDropdownMenu from './ExternalInvocationsDropdownMenu';

const hoisted = vi.hoisted(() => ({
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

vi.mock('sonner', () => ({
    toast: hoisted.toast,
}));

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'External invocation actions'}));

    return screen.findByRole('menu');
};

const menuItem = (name: string) => screen.getByRole('menuitem', {name});

describe('ExternalInvocationsDropdownMenu', () => {
    beforeEach(() => {
        hoisted.mutate.mockClear();
        hoisted.toast.mockClear();
    });

    it('opens the workflow run of the call when View is chosen', async () => {
        const onViewJob = vi.fn();

        render(<ExternalInvocationsDropdownMenu jobId="7" onViewJob={onViewJob} outcome="SUCCESS" />);

        await openMenu();
        await userEvent.click(menuItem('View'));

        expect(onViewJob).toHaveBeenCalledWith(7);
    });

    it('disables View and Stop for a call that started no workflow run', async () => {
        render(<ExternalInvocationsDropdownMenu jobId={null} onViewJob={vi.fn()} outcome="SUSPENDED" />);

        await openMenu();

        expect(menuItem('View')).toHaveAttribute('aria-disabled', 'true');
        expect(menuItem('Stop')).toHaveAttribute('aria-disabled', 'true');
    });

    it.each(['APPROVAL_REQUIRED', 'SUSPENDED', 'TIMEOUT'])(
        'stops the workflow run of a call that ended with %s',
        async (outcome) => {
            render(<ExternalInvocationsDropdownMenu jobId="7" onViewJob={vi.fn()} outcome={outcome} />);

            await openMenu();
            await userEvent.click(menuItem('Stop'));

            expect(hoisted.mutate).toHaveBeenCalledWith(7);
        }
    );

    it.each(['SUCCESS', 'ERROR', 'APPROVAL_DENIED'])(
        'disables Stop for a call whose workflow run ended with %s',
        async (outcome) => {
            render(<ExternalInvocationsDropdownMenu jobId="7" onViewJob={vi.fn()} outcome={outcome} />);

            await openMenu();

            expect(menuItem('Stop')).toHaveAttribute('aria-disabled', 'true');
        }
    );

    it('announces the stop once the workflow run is stopped', () => {
        render(<ExternalInvocationsDropdownMenu jobId="7" onViewJob={vi.fn()} outcome="SUSPENDED" />);

        hoisted.stopJobMutationOptions.onSuccess?.();

        expect(hoisted.toast).toHaveBeenCalledWith('Stopping Workflow Execution');
    });
});
