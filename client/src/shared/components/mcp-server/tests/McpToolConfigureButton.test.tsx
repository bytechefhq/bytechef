import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import McpToolConfigureButton from '../McpToolConfigureButton';

describe('McpToolConfigureButton', () => {
    it('is named after its tool and calls the handler', async () => {
        const user = userEvent.setup();
        const handleClick = vi.fn();

        render(<McpToolConfigureButton label="Lead intake" onClick={handleClick} />);

        await user.click(screen.getByRole('button', {name: 'Configure Lead intake'}));

        expect(handleClick).toHaveBeenCalledTimes(1);
    });
});
