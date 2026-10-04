import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ApprovalTaskList from '../ApprovalTaskList';

const {navigateMock} = vi.hoisted(() => ({navigateMock: vi.fn()}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

vi.mock('../../stores/useApprovalTasksStore', () => ({
    useApprovalTasksStore: (
        selector: (state: {
            hasActiveFilters: () => boolean;
            searchQuery: string;
            selectedApprovalTaskId: string | null;
        }) => unknown
    ) => selector({hasActiveFilters: () => false, searchQuery: '', selectedApprovalTaskId: null}),
}));

vi.mock('../hooks/useApprovalTaskList', () => ({
    useApprovalTaskList: () => ({
        emptyStateMessage: 'No approval tasks yet',
        filteredApprovalTasks: [],
        handleClearFilters: vi.fn(),
        handleSelectApprovalTask: vi.fn(),
        handleSortChange: vi.fn(),
        handleStatusToggle: vi.fn(),
        headerText: 'Open approval tasks',
        sortBy: 'created',
        sortDirection: 'desc',
        totalApprovalTaskCount: 0,
    }),
}));

vi.mock('../ApprovalTaskActiveFilterBadges', () => ({default: () => null}));
vi.mock('../ApprovalTaskFilters', () => ({default: () => null}));
vi.mock('../ApprovalTaskSearch', () => ({default: () => null}));
vi.mock('../ApprovalTaskSortMenu', () => ({default: () => null}));

describe('ApprovalTaskList', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('navigates back to automation from the header back button', async () => {
        const user = userEvent.setup();

        render(
            <MemoryRouter>
                <ApprovalTaskList />
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button', {name: 'Back'}));

        expect(navigateMock).toHaveBeenCalledWith('/automation');
    });
});
