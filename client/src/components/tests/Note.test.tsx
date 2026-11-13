import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import {Note} from '../Note';

describe('Note', () => {
    it('renders no close button without an onClose handler', () => {
        render(<Note content="Some hint" />);

        expect(screen.queryByRole('button', {name: 'Close'})).not.toBeInTheDocument();
    });

    it('calls onClose when the close button is clicked', async () => {
        const onClose = vi.fn();

        render(<Note content="Some hint" onClose={onClose} />);

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
