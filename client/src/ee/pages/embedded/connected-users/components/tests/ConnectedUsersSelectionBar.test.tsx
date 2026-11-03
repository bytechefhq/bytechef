import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import ConnectedUsersSelectionBar from '../ConnectedUsersSelectionBar';

describe('ConnectedUsersSelectionBar', () => {
    it('should render nothing when no connected user is selected', () => {
        const {container} = render(
            <ConnectedUsersSelectionBar
                count={0}
                onClear={vi.fn()}
                onUpgradeAutomation={vi.fn()}
                onUpgradeIntegration={vi.fn()}
            />
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('should show the selected count', () => {
        render(
            <ConnectedUsersSelectionBar
                count={2}
                onClear={vi.fn()}
                onUpgradeAutomation={vi.fn()}
                onUpgradeIntegration={vi.fn()}
            />
        );

        expect(screen.getByText('2 selected')).toBeInTheDocument();
    });

    it('should call the matching handler for each button', async () => {
        const onClear = vi.fn();
        const onUpgradeAutomation = vi.fn();
        const onUpgradeIntegration = vi.fn();

        render(
            <ConnectedUsersSelectionBar
                count={2}
                onClear={onClear}
                onUpgradeAutomation={onUpgradeAutomation}
                onUpgradeIntegration={onUpgradeIntegration}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Upgrade integration'}));
        await userEvent.click(screen.getByRole('button', {name: 'Upgrade automation'}));
        await userEvent.click(screen.getByRole('button', {name: 'Clear'}));

        expect(onUpgradeIntegration).toHaveBeenCalledTimes(1);
        expect(onUpgradeAutomation).toHaveBeenCalledTimes(1);
        expect(onClear).toHaveBeenCalledTimes(1);
    });
});
