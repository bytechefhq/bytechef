import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ComponentVisibilityTab from './ComponentVisibilityTab';

const mutateMock = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useComponentPoliciesQuery: () => ({
        data: {
            componentPolicies: [
                {enabled: true, icon: null, name: 'mailchimp', title: 'Mailchimp', version: 1},
                {enabled: false, icon: null, name: 'slack', title: 'Slack', version: 1},
            ],
        },
        error: null,
        isLoading: false,
    }),
    useUpdateComponentPolicyMutation: () => ({mutate: mutateMock}),
}));

const renderWithProviders = (ui: ReactNode) => {
    const QueryClientWrapper = createTestQueryClientWrapper();

    return render(<QueryClientWrapper>{ui}</QueryClientWrapper>);
};

describe('ComponentVisibilityTab', () => {
    beforeEach(() => {
        mutateMock.mockClear();
    });

    it('renders a switch per component reflecting enabled state', () => {
        renderWithProviders(<ComponentVisibilityTab />);

        const switches = screen.getAllByRole('switch');

        expect(switches).toHaveLength(2);
    });

    it('calls the mutation when a switch is toggled', async () => {
        renderWithProviders(<ComponentVisibilityTab />);

        const slackSwitch = screen.getByRole('switch', {name: /slack/i});

        await userEvent.click(slackSwitch);

        expect(mutateMock).toHaveBeenCalledWith(expect.objectContaining({enabled: true, name: 'slack'}));
    });
});
