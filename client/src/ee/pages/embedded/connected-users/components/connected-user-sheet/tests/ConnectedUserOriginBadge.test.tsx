import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectedUserOriginBadge from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserOriginBadge';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

const renderBadge = (provisioned: boolean) =>
    render(
        <TooltipProvider>
            <ConnectedUserOriginBadge provisioned={provisioned} resourceName="integration" />
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
});
