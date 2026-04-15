import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import ConnectionScopeBadge from './ConnectionScopeBadge';

const renderBadge = (props: Parameters<typeof ConnectionScopeBadge>[0]) =>
    render(
        <TooltipProvider>
            <ConnectionScopeBadge {...props} />
        </TooltipProvider>
    );

describe('ConnectionScopeBadge', () => {
    it('renders the visibility label for each scope', () => {
        const {rerender} = renderBadge({visibility: 'PRIVATE'});

        expect(screen.getByText('Private')).toBeInTheDocument();

        rerender(
            <TooltipProvider>
                <ConnectionScopeBadge visibility="WORKSPACE" />
            </TooltipProvider>
        );

        expect(screen.getByText('Workspace')).toBeInTheDocument();
    });

    it('renders without crashing when sharedProjectNames is empty', () => {
        renderBadge({sharedProjectNames: [], visibility: 'PROJECT'});

        expect(screen.getByText('Project')).toBeInTheDocument();
    });

    it('renders without crashing when sharedProjectNames is undefined for PROJECT visibility', () => {
        renderBadge({visibility: 'PROJECT'});

        expect(screen.getByText('Project')).toBeInTheDocument();
    });

    it('handles unknown visibility gracefully by falling back to PRIVATE label', () => {
        renderBadge({visibility: 'LEGACY_VALUE' as never});

        expect(screen.getByText('Private')).toBeInTheDocument();
    });
});
