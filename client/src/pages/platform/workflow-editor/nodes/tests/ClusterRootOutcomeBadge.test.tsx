import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import ClusterRootOutcomeBadge from '../ClusterRootOutcomeBadge';

describe('ClusterRootOutcomeBadge', () => {
    it('renders Blocked with the ban shield', () => {
        const {container} = render(<ClusterRootOutcomeBadge outcome="BLOCKED" />);

        expect(screen.getByTestId('cluster-root-outcome-badge')).toHaveTextContent('Blocked');
        expect(screen.getByTestId('cluster-root-outcome-badge')).toHaveClass('border-amber-500', 'text-amber-500');
        expect(container.querySelector('svg.lucide-shield-ban')).not.toBeNull();
    });

    it('renders Withheld with the alert shield', () => {
        const {container} = render(<ClusterRootOutcomeBadge outcome="WITHHELD" />);

        expect(screen.getByTestId('cluster-root-outcome-badge')).toHaveTextContent('Withheld');
        expect(container.querySelector('svg.lucide-shield-alert')).not.toBeNull();
    });
});
