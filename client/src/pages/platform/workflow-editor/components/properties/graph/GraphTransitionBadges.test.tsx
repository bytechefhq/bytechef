import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import GraphTransitionBadges from './GraphTransitionBadges';

interface RenderBadgesPropsI {
    dangling: Array<string>;
    dynamic: boolean;
    targets: Array<string>;
}

function renderBadges({dangling, dynamic, targets}: RenderBadgesPropsI) {
    return render(
        <TooltipProvider>
            <GraphTransitionBadges dangling={dangling} dynamic={dynamic} targets={targets} />
        </TooltipProvider>
    );
}

describe('GraphTransitionBadges', () => {
    it('should render nothing for a terminal node', () => {
        const {container} = renderBadges({dangling: [], dynamic: false, targets: []});

        expect(container).toBeEmptyDOMElement();
    });

    it('should render a badge per static target', () => {
        renderBadges({dangling: [], dynamic: false, targets: ['approve', 'reject']});

        expect(screen.getByText('approve')).toBeInTheDocument();
        expect(screen.getByText('reject')).toBeInTheDocument();
        expect(screen.queryByText('dynamic')).not.toBeInTheDocument();
    });

    it('should render the dynamic marker when the expression is not a bare literal', () => {
        renderBadges({dangling: [], dynamic: true, targets: ['approve']});

        expect(screen.getByText('dynamic')).toBeInTheDocument();
    });

    it('should render a warning badge per dangling target', () => {
        renderBadges({dangling: ['missingNode'], dynamic: false, targets: []});

        expect(screen.getByText('missingNode')).toBeInTheDocument();
    });

    it('should render targets, the dynamic marker, and dangling warnings together', () => {
        renderBadges({dangling: ['missingNode'], dynamic: true, targets: ['approve']});

        expect(screen.getByText('approve')).toBeInTheDocument();
        expect(screen.getByText('dynamic')).toBeInTheDocument();
        expect(screen.getByText('missingNode')).toBeInTheDocument();
    });
});
