import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import WorkflowTestNodeStatus from '../WorkflowTestNodeStatus';

describe('WorkflowTestNodeStatus', () => {
    it('shows a shield badge and no duration for a blocked state', () => {
        const {container} = render(<WorkflowTestNodeStatus testNodeState={{durationMillis: 12, status: 'BLOCKED'}} />);

        const badge = screen.getByTitle('Blocked');

        expect(badge).toHaveClass('border-amber-500', 'text-amber-500');
        expect(container.querySelector('svg.lucide-shield-ban')).not.toBeNull();
        expect(screen.queryByText('12ms')).toBeNull();
    });

    it('shows an alert shield badge and no duration for a withheld state', () => {
        const {container} = render(<WorkflowTestNodeStatus testNodeState={{durationMillis: 12, status: 'WITHHELD'}} />);

        expect(screen.getByTitle('Withheld')).toHaveClass('border-amber-500', 'text-amber-500');
        expect(container.querySelector('svg.lucide-shield-alert')).not.toBeNull();
        expect(screen.queryByText('12ms')).toBeNull();
    });

    it('shows an eraser badge and no duration for a redacted state', () => {
        const {container} = render(<WorkflowTestNodeStatus testNodeState={{durationMillis: 12, status: 'REDACTED'}} />);

        expect(screen.getByTitle('Redacted')).toHaveClass('border-sky-500', 'text-sky-500');
        expect(container.querySelector('svg.lucide-eraser')).not.toBeNull();
        expect(screen.queryByText('12ms')).toBeNull();
    });

    it('titles a failed state with its error and keeps the duration', () => {
        render(<WorkflowTestNodeStatus testNodeState={{durationMillis: 12, error: 'boom', status: 'FAILED'}} />);

        expect(screen.getByTitle('boom')).toHaveClass('border-red-500');
        expect(screen.getByText('12ms')).toBeDefined();
    });
});
