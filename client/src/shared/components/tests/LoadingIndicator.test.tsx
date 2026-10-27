import {TooltipProvider} from '@/components/ui/tooltip';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {expect, it} from 'vitest';

screen.debug();

const renderLoadingIndicator = (isFetching: number, isOnline: boolean, showLabel = false) => {
    render(
        <TooltipProvider>
            <LoadingIndicator isFetching={isFetching} isOnline={isOnline} showLabel={showLabel} />
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

it('should show a saving label while saving when the label is enabled', () => {
    renderLoadingIndicator(1, true, true);

    expect(screen.getByLabelText('Loading indicator')).toHaveTextContent('Saving…');
});

it('should show an offline label when offline and the label is enabled', () => {
    renderLoadingIndicator(0, false, true);

    expect(screen.getByLabelText('Loading indicator')).toHaveTextContent('Offline');
});

it('should show no label while saving when the label is disabled', () => {
    renderLoadingIndicator(1, true);

    expect(screen.getByLabelText('Loading indicator')).toHaveTextContent(/^$/);
});
