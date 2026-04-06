import {TooltipProvider} from '@/components/ui/tooltip';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {expect, it} from 'vitest';

screen.debug();

const renderLoadingIndicator = (isFetching: number, isOnline: boolean) => {
    render(
        <TooltipProvider>
            <LoadingIndicator isFetching={isFetching} isOnline={isOnline} />
        </TooltipProvider>
    );
};

it('should render the success message when it is not fetching and isOnline is true ', async () => {
    renderLoadingIndicator(0, true);

    expect(screen.queryByText('All changes are saved')).not.toBeInTheDocument();

    await userEvent.hover(screen.getByLabelText('Loading indicator'));

    await waitFor(() => {
        expect(screen.getByRole('tooltip', {hidden: true, name: 'All changes are saved'})).toBeVisible();
    });
});

it('should render the fetching message when something is fetching and isOnline is true ', async () => {
    renderLoadingIndicator(4, true);

    expect(screen.queryByText('Saving your progress')).not.toBeInTheDocument();

    await userEvent.hover(screen.getByLabelText('Loading indicator'));

    await waitFor(() => {
        expect(screen.getByRole('tooltip', {hidden: true, name: 'Saving your progress'})).toBeVisible();
    });
});

it('should render the offline message when isOnline is false ', async () => {
    renderLoadingIndicator(0, false);

    expect(screen.queryByText('You are offline')).not.toBeInTheDocument();

    await userEvent.hover(screen.getByLabelText('Loading indicator'));

    await waitFor(() => {
        expect(screen.getByRole('tooltip', {hidden: true, name: 'You are offline'})).toBeVisible();
    });
});
