import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {TrialBanner} from './TrialBanner';

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
    subscription: undefined as
        | {currentPeriodEnd?: Date; planName?: string; status?: string; taskLimit?: number; tasksUsed?: number}
        | undefined,
}));

vi.mock('@/shared/queries/platform/billing.queries', () => ({
    useGetCurrentSubscriptionQuery: () => ({data: hoisted.subscription}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

describe('TrialBanner', () => {
    beforeEach(() => {
        hoisted.navigate.mockClear();
        hoisted.subscription = undefined;
    });

    it('renders nothing when there is no subscription', () => {
        const {container} = render(<TrialBanner />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing for a non-trial plan', () => {
        hoisted.subscription = {planName: 'STARTER', status: 'ACTIVE'};

        const {container} = render(<TrialBanner />);

        expect(container).toBeEmptyDOMElement();
    });

    it('shows days remaining and task usage for an active trial', () => {
        hoisted.subscription = {
            currentPeriodEnd: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000),
            planName: 'TRIAL',
            status: 'ACTIVE',
            taskLimit: 5000,
            tasksUsed: 120,
        };

        render(<TrialBanner />);

        expect(screen.getByText(/3 days remaining/)).toBeInTheDocument();
        expect(screen.getByText(/120\/5000 tasks used/)).toBeInTheDocument();
    });

    it('shows an expired message when the trial subscription is canceled', () => {
        hoisted.subscription = {planName: 'TRIAL', status: 'CANCELED'};

        render(<TrialBanner />);

        expect(screen.getByText('Your trial has expired.')).toBeInTheDocument();
    });

    it('navigates to billing settings when the upgrade button is clicked', () => {
        hoisted.subscription = {planName: 'TRIAL', status: 'CANCELED'};

        render(<TrialBanner />);

        fireEvent.click(screen.getByText('Upgrade'));

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/settings/billing');
    });
});
