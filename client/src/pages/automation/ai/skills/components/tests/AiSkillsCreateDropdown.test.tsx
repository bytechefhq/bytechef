import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AiSkillsCreateDropdown from '../AiSkillsCreateDropdown';

vi.mock('@/pages/automation/ai/skills/components/AiSkillUploadDialog', () => ({
    default: () => null,
}));

vi.mock('@/pages/automation/ai/skills/components/AiSkillWriteDialog', () => ({
    default: () => null,
}));

vi.mock('@/components/ui/dropdown-menu', () => ({
    DropdownMenu: ({children}: {children: React.ReactNode}) => <div data-testid="dropdown-menu">{children}</div>,
    DropdownMenuContent: ({children}: {children: React.ReactNode}) => (
        <div data-testid="dropdown-content">{children}</div>
    ),
    DropdownMenuItem: ({children, onClick}: {children: React.ReactNode; className?: string; onClick?: () => void}) => (
        <button data-testid="dropdown-item" onClick={onClick}>
            {children}
        </button>
    ),
    DropdownMenuTrigger: ({children}: {asChild?: boolean; children: React.ReactNode}) => (
        <div data-testid="dropdown-trigger">{children}</div>
    ),
}));

const renderComponent = () =>
    render(
        <MemoryRouter>
            <AiSkillsCreateDropdown />
        </MemoryRouter>
    );

describe('AiSkillsCreateDropdown', () => {
    beforeEach(() => {
        applicationInfoStore.setState((state) => ({
            ...state,
            ai: {...state.ai, copilot: {enabled: false}},
            featureFlags: {'ff-4554': true},
        }));
    });

    afterEach(() => {
        resetAll();

        applicationInfoStore.setState((state) => ({
            ...state,
            ai: {...state.ai, copilot: {enabled: false}},
            featureFlags: {},
        }));
    });

    it('does not render the Create With AI item when copilot is disabled', () => {
        renderComponent();

        expect(screen.queryByText('Create With AI')).not.toBeInTheDocument();
    });

    it('renders the Create With AI item when copilot is enabled', () => {
        applicationInfoStore.setState((state) => ({
            ...state,
            ai: {...state.ai, copilot: {enabled: true}},
        }));

        renderComponent();

        expect(screen.getByText('Create With AI')).toBeInTheDocument();
    });
});
