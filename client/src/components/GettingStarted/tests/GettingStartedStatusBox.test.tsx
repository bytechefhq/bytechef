import GettingStartedStatusBox from '@/components/GettingStarted/GettingStartedStatusBox';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

describe('GettingStartedStatusBox', () => {
    it('renders the title, children and test id', () => {
        render(
            <GettingStartedStatusBox data-testid="status-box" title="Call received" tone="success">
                <p>Details</p>
            </GettingStartedStatusBox>
        );

        expect(screen.getByTestId('status-box')).toBeInTheDocument();
        expect(screen.getByText('Call received')).toBeInTheDocument();
        expect(screen.getByText('Details')).toBeInTheDocument();
    });

    it('pulses the dot only while waiting', () => {
        const {container, rerender} = render(<GettingStartedStatusBox title="Listening" tone="waiting" />);

        expect(container.querySelector('span[aria-hidden="true"]')).toHaveClass('animate-pulse');

        rerender(<GettingStartedStatusBox title="Done" tone="success" />);

        expect(container.querySelector('span[aria-hidden="true"]')).not.toHaveClass('animate-pulse');
    });

    it('uses the destructive color for the error tone', () => {
        const {container} = render(<GettingStartedStatusBox title="Failed" tone="error" />);

        expect(container.querySelector('span[aria-hidden="true"]')).toHaveClass('bg-surface-destructive-primary');
    });
});
