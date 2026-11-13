import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import PublishPopover from '../PublishPopover';

const renderPublishPopover = (props: Partial<React.ComponentProps<typeof PublishPopover>> = {}) => {
    const onPublishSubmitMock = vi.fn();

    render(
        <TooltipProvider>
            <PublishPopover
                isPending={false}
                onPublishSubmit={onPublishSubmitMock}
                title="Publish Project"
                tooltip="Publish the project"
                {...props}
            />
        </TooltipProvider>
    );

    return {onPublishSubmitMock};
};

describe('PublishPopover', () => {
    it('submits the description typed into the placeholder-guided field', async () => {
        const {onPublishSubmitMock} = renderPublishPopover();

        await userEvent.click(screen.getByRole('button', {name: /publish/i}));

        const descriptionField = screen.getByPlaceholderText(/Describe what changed in this version/);

        await userEvent.type(descriptionField, 'Added a Slack notification');
        await userEvent.click(screen.getByRole('button', {name: 'Publish button'}));

        await vi.waitFor(() => {
            expect(onPublishSubmitMock).toHaveBeenCalledWith(
                expect.objectContaining({description: 'Added a Slack notification'})
            );
        });
    });

    it('shows the given title once opened', async () => {
        renderPublishPopover({title: 'Publish Integration', tooltip: 'Publish the integration'});

        await userEvent.click(screen.getByRole('button', {name: /publish/i}));

        expect(screen.getByRole('heading', {name: 'Publish Integration'})).toBeInTheDocument();
    });

    it('closes the popover and clears the description when the publish succeeds', async () => {
        const {onPublishSubmitMock} = renderPublishPopover();

        onPublishSubmitMock.mockImplementation(({onSuccess}: {onSuccess: () => void}) => onSuccess());

        await userEvent.click(screen.getByRole('button', {name: /publish/i}));
        await userEvent.type(screen.getByRole('textbox'), 'Added a Slack notification');
        await userEvent.click(screen.getByRole('button', {name: 'Publish button'}));

        await vi.waitFor(() => {
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        });
    });

    it('renders a disabled trigger without a popover when there is nothing to publish', async () => {
        renderPublishPopover({disabled: true});

        const triggerButton = screen.getByRole('button', {name: /publish/i});

        expect(triggerButton).toBeDisabled();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });

    it('applies the trigger and submit button class overrides', async () => {
        renderPublishPopover({submitButtonClassName: 'submit-override', triggerButtonClassName: 'trigger-override'});

        const triggerButton = screen.getByRole('button', {name: /publish/i});

        expect(triggerButton).toHaveClass('trigger-override');

        await userEvent.click(triggerButton);

        expect(screen.getByRole('button', {name: 'Publish button'})).toHaveClass('submit-override');
    });
});
