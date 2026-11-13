import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import PublishPopover from '../PublishPopover';

describe('PublishPopover', () => {
    it('submits the description typed into the placeholder-guided field', async () => {
        const onPublishProjectSubmitMock = vi.fn();

        render(
            <TooltipProvider>
                <PublishPopover isPending={false} onPublishProjectSubmit={onPublishProjectSubmitMock} />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByRole('button', {name: /publish/i}));

        const descriptionField = screen.getByPlaceholderText(/Describe what changed in this version/);

        await userEvent.type(descriptionField, 'Added a Slack notification');
        await userEvent.click(screen.getByRole('button', {name: 'Publish button'}));

        await vi.waitFor(() => {
            expect(onPublishProjectSubmitMock).toHaveBeenCalledWith(
                expect.objectContaining({description: 'Added a Slack notification'})
            );
        });
    });
});
