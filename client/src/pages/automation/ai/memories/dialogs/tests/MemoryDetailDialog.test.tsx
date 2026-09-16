import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiAutoMemoryDetailI} from '../../hooks/useAiAutoMemories';
import MemoryDetailDialog from '../MemoryDetailDialog';

vi.mock('@/pages/automation/ai/memories/hooks/useAiAutoMemories', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/pages/automation/ai/memories/hooks/useAiAutoMemories')>()),
    useAiAutoMemoryDetailQuery: vi.fn(),
}));

const {useAiAutoMemoryDetailQuery} = await import('@/pages/automation/ai/memories/hooks/useAiAutoMemories');

const mockUseMemoryDetailQuery = vi.mocked(useAiAutoMemoryDetailQuery);

const makeMemory = (overrides: Partial<AiAutoMemoryDetailI> = {}): AiAutoMemoryDetailI => ({
    content: '# User profile\n\nAlice prefers concise replies.',
    createdAt: '2026-04-01T00:00:00Z',
    description: 'User profile summary',
    environmentId: 0,
    id: 1,
    memoryType: 'USER',
    name: 'user_profile',
    principalId: 42,
    principalType: 'USER',
    title: 'User profile',
    updatedAt: '2026-04-10T12:00:00Z',
    version: 3,
    workspaceId: 1,
    ...overrides,
});

const stubDetail = (data: AiAutoMemoryDetailI | null | undefined = undefined, isPending = false) =>
    mockUseMemoryDetailQuery.mockImplementation(((memory: AiAutoMemoryDetailI | null) => ({
        data: data === undefined ? (memory ?? null) : data,
        isPending,
    })) as never);

beforeEach(() => {
    stubDetail();
});

describe('MemoryDetailDialog', () => {
    it('renders title, description, and markdown content when open', () => {
        render(<MemoryDetailDialog memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={1} />);

        expect(screen.getByRole('heading', {level: 2, name: /user profile/i})).toBeInTheDocument();
        expect(screen.getByText(/user profile summary/i)).toBeInTheDocument();
        expect(screen.getByText(/alice prefers concise replies/i)).toBeInTheDocument();
    });

    it('shows the memory type badge by its label rather than the raw enum value', () => {
        render(
            <MemoryDetailDialog
                memory={makeMemory({memoryType: 'FEEDBACK'})}
                onClose={vi.fn()}
                open={true}
                workspaceId={1}
            />
        );

        expect(screen.getByText('Feedback')).toBeInTheDocument();
        expect(screen.queryByText('FEEDBACK')).toBeNull();
    });

    it('invokes onClose when Close is clicked', async () => {
        const onClose = vi.fn();

        render(<MemoryDetailDialog memory={makeMemory()} onClose={onClose} open={true} workspaceId={1} />);

        await userEvent.click(screen.getByRole('button', {name: /close/i}));

        expect(onClose).toHaveBeenCalled();
    });

    it('renders nothing interactive when open is false', () => {
        render(<MemoryDetailDialog memory={makeMemory()} onClose={vi.fn()} open={false} workspaceId={1} />);

        expect(screen.queryByText(/alice prefers concise replies/i)).toBeNull();
    });
});

describe('MemoryDetailDialog content loading', () => {
    it('says the content is loading while the detail query is still pending', () => {
        stubDetail(null, true);

        render(<MemoryDetailDialog memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={1} />);

        expect(screen.getByText(/loading memory content/i)).toBeInTheDocument();
        expect(screen.queryByText(/alice prefers concise replies/i)).toBeNull();
    });

    it('says the memory is gone when the detail query resolves to nothing', () => {
        stubDetail(null);

        render(<MemoryDetailDialog memory={makeMemory()} onClose={vi.fn()} open={true} workspaceId={1} />);

        expect(screen.getByText(/no longer available/i)).toBeInTheDocument();
        expect(screen.queryByText(/loading memory content/i)).toBeNull();
    });
});

describe('MemoryDetailDialog timestamps and dismissal', () => {
    it('falls back to the raw value when the updated timestamp is malformed', () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});

        render(
            <MemoryDetailDialog
                memory={makeMemory({updatedAt: 'not-a-date'})}
                onClose={vi.fn()}
                open={true}
                workspaceId={1}
            />
        );

        expect(screen.getByText(/updated not-a-date/i)).toBeInTheDocument();
        expect(warn).toHaveBeenCalledWith(
            'MemoryDetailDialog: formatTimestamp failed',
            expect.objectContaining({value: 'not-a-date'})
        );

        warn.mockRestore();
    });

    it('calls onClose when the dialog is dismissed with Escape', async () => {
        const onClose = vi.fn();

        render(<MemoryDetailDialog memory={makeMemory()} onClose={onClose} open={true} workspaceId={1} />);

        await userEvent.keyboard('{Escape}');

        expect(onClose).toHaveBeenCalled();
    });
});
