import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import ResourceVisibilityBadge from './ResourceVisibilityBadge';

const renderBadge = (props: Parameters<typeof ResourceVisibilityBadge>[0]) =>
    render(
        <TooltipProvider>
            <ResourceVisibilityBadge {...props} />
        </TooltipProvider>
    );

describe('ResourceVisibilityBadge', () => {
    it('renders the visibility label for each scope', () => {
        const {rerender} = renderBadge({visibility: 'PRIVATE'});

        expect(screen.getByText('Private')).toBeInTheDocument();

        rerender(
            <TooltipProvider>
                <ResourceVisibilityBadge visibility="WORKSPACE" />
            </TooltipProvider>
        );

        expect(screen.getByText('Workspace')).toBeInTheDocument();

        rerender(
            <TooltipProvider>
                <ResourceVisibilityBadge visibility="ORGANIZATION" />
            </TooltipProvider>
        );

        expect(screen.getByText('Organization')).toBeInTheDocument();
    });

    it('handles unknown visibility gracefully by falling back to PRIVATE label', () => {
        renderBadge({visibility: 'LEGACY_VALUE' as never});

        expect(screen.getByText('Private')).toBeInTheDocument();
    });
});
