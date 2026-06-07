import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import {AiAutoMemoryI} from '../../hooks/useAiAutoMemories';
import MemoryDetailDialog from '../MemoryDetailDialog';

const makeMemory = (overrides: Partial<AiAutoMemoryI> = {}): AiAutoMemoryI => ({
    content: '# User profile\n\nAlice prefers concise replies.',
    createdAt: '2026-04-01T00:00:00Z',
    description: 'User profile summary',
    environmentId: 0,
    id: 1,
    memoryType: 'USER',
    name: 'user_profile',
    title: 'User profile',
    updatedAt: '2026-04-10T12:00:00Z',
    userId: 42,
    workspaceId: 1,
    ...overrides,
});

describe('MemoryDetailDialog', () => {
    it('renders title, description, and markdown content when open', () => {
        render(<MemoryDetailDialog memory={makeMemory()} onClose={vi.fn()} open={true} />);

        expect(screen.getByRole('heading', {level: 2, name: /user profile/i})).toBeInTheDocument();
        expect(screen.getByText(/user profile summary/i)).toBeInTheDocument();
        expect(screen.getByText(/alice prefers concise replies/i)).toBeInTheDocument();
    });

    it('shows the memory type badge', () => {
        render(<MemoryDetailDialog memory={makeMemory({memoryType: 'FEEDBACK'})} onClose={vi.fn()} open={true} />);

        expect(screen.getByText('FEEDBACK')).toBeInTheDocument();
    });

    it('invokes onClose when Close is clicked', async () => {
        const onClose = vi.fn();

        render(<MemoryDetailDialog memory={makeMemory()} onClose={onClose} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: /close/i}));

        expect(onClose).toHaveBeenCalled();
    });

    it('renders nothing interactive when open is false', () => {
        render(<MemoryDetailDialog memory={makeMemory()} onClose={vi.fn()} open={false} />);

        expect(screen.queryByText(/alice prefers concise replies/i)).toBeNull();
    });
});
