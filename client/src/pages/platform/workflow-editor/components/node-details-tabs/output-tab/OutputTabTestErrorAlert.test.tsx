import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import OutputTabTestErrorAlert from './OutputTabTestErrorAlert';

afterEach(() => {
    resetAll();
});

describe('OutputTabTestErrorAlert', () => {
    it('shows the title and message of the failure', () => {
        render(
            <OutputTabTestErrorAlert
                onDismiss={vi.fn()}
                testOutputError={{message: 'All scraping engines failed', title: 'Test failed'}}
            />
        );

        expect(screen.getByRole('alert')).toHaveTextContent('Test failed');
        expect(screen.getByText('All scraping engines failed')).toBeInTheDocument();
    });

    it('calls onDismiss when the dismiss button is clicked', async () => {
        const onDismiss = vi.fn();

        render(
            <OutputTabTestErrorAlert onDismiss={onDismiss} testOutputError={{message: 'x', title: 'Reset failed'}} />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Dismiss error'}));

        expect(onDismiss).toHaveBeenCalledTimes(1);
    });
});
