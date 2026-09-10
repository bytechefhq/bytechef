import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AiSkillsCreateDropdown from '../AiSkillsCreateDropdown';

vi.mock('@/pages/automation/ai/skills/components/AiSkillUploadDialog', () => ({
    default: ({onCreated}: {onCreated: (id: string | null) => void}) => (
        <button data-testid="upload-created" onClick={() => onCreated('99')} type="button" />
    ),
}));

vi.mock('@/pages/automation/ai/skills/components/AiSkillWriteDialog', () => ({
    default: ({onCreated}: {onCreated: (id: string | null) => void}) => (
        <button data-testid="write-created" onClick={() => onCreated(null)} type="button" />
    ),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
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

const {navigateMock} = vi.hoisted(() => ({navigateMock: vi.fn()}));

const renderComponent = (pathname = '/automation/settings/ai/skills', trigger?: React.ReactNode) =>
    render(
        <MemoryRouter initialEntries={[pathname]}>
            <AiSkillsCreateDropdown trigger={trigger} />
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

    it('opens the newly created skill on the mount the dropdown was used from', () => {
        renderComponent();

        screen.getByTestId('upload-created').click();

        expect(navigateMock).toHaveBeenCalledWith('/automation/settings/ai/skills/99');
    });

    it('keeps an embedded visitor on the embedded mount', () => {
        renderComponent('/embedded/settings/ai/skills');

        screen.getByTestId('upload-created').click();

        expect(navigateMock).toHaveBeenCalledWith('/embedded/settings/ai/skills/99');
    });

    it('stays put when the dialog reports no created skill', () => {
        renderComponent();

        screen.getByTestId('write-created').click();

        expect(navigateMock).not.toHaveBeenCalled();
    });

    it('renders a caller-supplied trigger instead of the default button', () => {
        renderComponent('/automation/settings/ai/skills', <button type="button">Add</button>);

        expect(screen.getByText('Add')).toBeInTheDocument();
        expect(screen.queryByText('Create Skill')).not.toBeInTheDocument();
    });
});
