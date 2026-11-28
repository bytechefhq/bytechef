import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeAll, describe, expect, it, vi} from 'vitest';

import EnvironmentPromotionConnectionRow from '../EnvironmentPromotionConnectionRow';

beforeAll(() => {
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const CONNECTION = {
    componentName: 'slack',
    connectionVersion: 1,
    sourceConnectionId: 'conn-1',
    sourceConnectionName: 'Slack Prod',
    suggestedTargetConnectionId: '9',
    targetCandidates: [
        {connectionId: '9', name: 'Slack Staging'},
        {connectionId: '10', name: 'Slack Staging (2)'},
    ],
    usedBy: [],
};

describe('EnvironmentPromotionConnectionRow', () => {
    it('lists exactly the target candidates and preselects the suggested one', async () => {
        const user = userEvent.setup();

        render(
            <EnvironmentPromotionConnectionRow
                connection={CONNECTION}
                onTargetConnectionIdChange={vi.fn()}
                targetConnectionId="9"
            />
        );

        expect(screen.getByText('Slack Staging')).toBeInTheDocument();

        await user.click(screen.getByLabelText('Target connection for Slack Prod'));

        expect(await screen.findAllByRole('option')).toHaveLength(2);
        expect(screen.getByRole('option', {name: 'Slack Staging (2)'})).toBeInTheDocument();
    });

    it('offers Unresolved only when there is no suggested target', async () => {
        const user = userEvent.setup();

        render(
            <EnvironmentPromotionConnectionRow
                connection={{...CONNECTION, suggestedTargetConnectionId: null}}
                onTargetConnectionIdChange={vi.fn()}
                targetConnectionId={undefined}
            />
        );

        await user.click(screen.getByLabelText('Target connection for Slack Prod'));

        expect(await screen.findByRole('option', {name: 'Unresolved'})).toBeInTheDocument();
        expect(screen.getAllByRole('option')).toHaveLength(3);
    });
});
