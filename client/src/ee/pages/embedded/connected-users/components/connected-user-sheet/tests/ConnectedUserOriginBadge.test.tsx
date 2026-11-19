import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectedUserOriginBadge from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserOriginBadge';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';

const renderBadge = (provisioned: boolean, provisionedBy?: string) =>
    render(
        <TooltipProvider>
            <ConnectedUserOriginBadge
                provisioned={provisioned}
                provisionedBy={provisionedBy}
                resourceName="integration"
            />
        </TooltipProvider>
    );

describe('ConnectedUserOriginBadge', () => {
    it('labels an admin-provisioned resource as provisioned', () => {
        renderBadge(true);

        expect(screen.getByText('Provisioned')).toBeInTheDocument();
    });

    it('labels a resource the connected user added as self-created', () => {
        renderBadge(false);

        expect(screen.getByText('Self-created')).toBeInTheDocument();
    });

    it('names the admin who provisioned the resource', async () => {
        renderBadge(true, 'admin@localhost.com');

        await userEvent.hover(screen.getByText('Provisioned'));

        expect(await screen.findByRole('tooltip')).toHaveTextContent(
            'admin@localhost.com provisioned this integration for the connected user.'
        );
    });
});
