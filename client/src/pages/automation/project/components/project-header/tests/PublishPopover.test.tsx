import {TooltipProvider} from '@/components/ui/tooltip';
import PublishPopover from '@/pages/automation/project/components/project-header/components/PublishPopover';
import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

screen.debug();

const mockOnPublishProjectSubmit = vi.fn();

const renderPublishPopover = (disabled = false) => {
    render(
        <TooltipProvider>
            <PublishPopover disabled={disabled} isPending={false} onPublishProjectSubmit={mockOnPublishProjectSubmit} />
        </TooltipProvider>
    );
};

it('should render the publish popover button', () => {
    renderPublishPopover();

    expect(screen.getByText('Publish')).toBeInTheDocument();
});

it('should open the publish popover on click', () => {
    renderPublishPopover();

    fireEvent.click(screen.getByText('Publish'));

    expect(screen.getByText('Publish Project')).toBeInTheDocument();
});

it('should call the onPublishProjectSubmit function with the correct description on submit', async () => {
    renderPublishPopover();

    fireEvent.click(screen.getByText('Publish'));

    fireEvent.input(screen.getByLabelText('Description'), {target: {value: 'Project description'}});

    expect(screen.getByLabelText('Publish button')).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Publish button'));

    await waitFor(() => {
        expect(mockOnPublishProjectSubmit).toHaveBeenCalledWith({
            description: 'Project description',
            onSuccess: expect.any(Function),
        });
    });
});

it('should disable the publish button when there is nothing to publish', () => {
    renderPublishPopover(true);

    expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
});

it('should not open the publish popover when the publish button is disabled', () => {
    renderPublishPopover(true);

    fireEvent.click(screen.getByText('Publish'));

    expect(screen.queryByText('Publish Project')).not.toBeInTheDocument();
});
