import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {expect, it} from 'vitest';
import LoadingIndicator from '@/shared/components/LoadingIndicator';

screen.debug();

const renderLoaderNotification = (isFetching: number, isOnline: boolean) => {
    render(
        <TooltipProvider>
            <LoadingIndicator isFetching={isFetching} isOnline={isOnline} />
        </TooltipProvider>
    );
};

it('should render the success message when it is not fetching and isOnline is true ', async () => {
    renderLoaderNotification(0, true);

    expect(screen.queryByText('All changes are saved')).not.toBeInTheDocument();

    await userEvent.hover(screen.getByLabelText('Loader notification indicator'));

    await waitFor(() => {
        expect(screen.getByRole('tooltip', {hidden: true, name: 'All changes are saved'})).toBeVisible();
    });
});

it('should render the fetching message when something is fetching and isOnline is true ', async () => {
    renderLoaderNotification(4, true);

    expect(screen.queryByText('Saving your progress')).not.toBeInTheDocument();

    await userEvent.hover(screen.getByLabelText('Loader notification indicator'));

    await waitFor(() => {
        expect(screen.getByRole('tooltip', {hidden: true, name: 'Saving your progress'})).toBeVisible();
    });
});

it('should render the offline message when isOnline is false ', async () => {
    renderLoaderNotification(0, false);

    expect(screen.queryByText('You are offline')).not.toBeInTheDocument();

    await userEvent.hover(screen.getByLabelText('Loader notification indicator'));

    await waitFor(() => {
        expect(screen.getByRole('tooltip', {hidden: true, name: 'You are offline'})).toBeVisible();
    });
});
