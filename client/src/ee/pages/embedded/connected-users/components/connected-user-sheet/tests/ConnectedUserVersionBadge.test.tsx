import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';

import ConnectedUserVersionBadge from '../ConnectedUserVersionBadge';

const renderBadge = (newerVersionAvailable?: boolean) =>
    render(
        <TooltipProvider>
            <ConnectedUserVersionBadge
                newerVersionAvailable={newerVersionAvailable}
                tooltip="The integration version"
                version={2}
            />
        </TooltipProvider>
    );

describe('ConnectedUserVersionBadge', () => {
    it('shows the version without a dot when no newer version is available', () => {
        renderBadge();

        expect(screen.getByText('V2')).toBeInTheDocument();
        expect(screen.queryByRole('img', {name: 'Newer version available'})).not.toBeInTheDocument();
    });

    it('shows a dot and explains it in the tooltip when a newer version is available', async () => {
        renderBadge(true);

        expect(screen.getByRole('img', {name: 'Newer version available'})).toBeInTheDocument();

        await userEvent.hover(screen.getByText('V2'));

        expect(await screen.findByRole('tooltip')).toHaveTextContent('The integration versionNewer version available');
    });
});
