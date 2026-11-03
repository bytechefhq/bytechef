import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import SigningSecretDialog from '../SigningSecretDialog';

describe('SigningSecretDialog', () => {
    it('shows the secret and copies it', async () => {
        const writeText = vi.fn().mockResolvedValue(undefined);

        Object.defineProperty(navigator, 'clipboard', {configurable: true, value: {writeText}});

        render(<SigningSecretDialog onClose={vi.fn()} signingSecret="whsec_abc123" />);

        expect(screen.getByText("Copy this secret now. You won't be able to see it again.")).toBeInTheDocument();
        expect(screen.getByDisplayValue('whsec_abc123')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: /Copy/}));

        expect(writeText).toHaveBeenCalledWith('whsec_abc123');
    });
});
