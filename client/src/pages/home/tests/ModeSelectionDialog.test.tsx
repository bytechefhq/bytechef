import ModeSelectionDialog from '@/pages/home/components/ModeSelectionDialog';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {Mock, afterEach, beforeEach, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    currentEnvironmentId: 0,
    navigateMock: vi.fn(),
    setCurrentTypeMock: vi.fn(),
}));

vi.mock('@/pages/home/stores/usePlatformTypeStore', async () => {
    const actual = await import('@/pages/home/stores/usePlatformTypeStore');

    return {
        ...actual,
        usePlatformTypeStore: vi.fn(),
    };
});

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: hoisted.currentEnvironmentId})
    ),
}));

vi.mock('react-router-dom', async () => {
    const actual = await import('react-router-dom');

    return {
        ...actual,
        useNavigate: () => hoisted.navigateMock,
    };
});

const handleDialogCloseMock = vi.fn();

beforeEach(() => {
    hoisted.currentEnvironmentId = 0;

    (usePlatformTypeStore as unknown as Mock).mockImplementation(
        (selector: (state: {currentType: undefined; setCurrentType: Mock}) => unknown) =>
            selector({currentType: undefined, setCurrentType: hoisted.setCurrentTypeMock})
    );
});

afterEach(() => {
    resetAll();
});

const renderModeSelectionDialog = (isDialogOpen = true) =>
    render(
        <MemoryRouter>
            <ModeSelectionDialog handleDialogClose={handleDialogCloseMock} isDialogOpen={isDialogOpen} />
        </MemoryRouter>
    );

it('shows an overview of both modes when open', () => {
    renderModeSelectionDialog();

    expect(screen.getByText('How will you use ByteChef?')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Start with Automation'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Start with Embedded'})).toBeInTheDocument();
    expect(screen.getByText('Embed a white-labeled integration marketplace')).toBeInTheDocument();
});

it('renders nothing when closed', () => {
    renderModeSelectionDialog(false);

    expect(screen.queryByText('How will you use ByteChef?')).not.toBeInTheDocument();
});

it('cannot be dismissed without choosing a mode', async () => {
    renderModeSelectionDialog();

    await userEvent.keyboard('[Escape]');

    expect(handleDialogCloseMock).not.toHaveBeenCalled();
    expect(screen.queryByRole('button', {name: 'Close'})).not.toBeInTheDocument();
});

it('selects Embedded and lets the embedded index route decide where to land', async () => {
    renderModeSelectionDialog();

    await userEvent.click(screen.getByRole('button', {name: 'Start with Embedded'}));

    expect(hoisted.setCurrentTypeMock).toHaveBeenCalledWith(PlatformType.EMBEDDED);
    expect(handleDialogCloseMock).toHaveBeenCalledOnce();
    expect(hoisted.navigateMock).toHaveBeenCalledWith('/embedded');
});

it('selects Automation and redirects to its projects page', async () => {
    renderModeSelectionDialog();

    await userEvent.click(screen.getByRole('button', {name: 'Start with Automation'}));

    expect(hoisted.setCurrentTypeMock).toHaveBeenCalledWith(PlatformType.AUTOMATION);
    expect(hoisted.navigateMock).toHaveBeenCalledWith('/automation/projects');
});

it('redirects to the deployed view outside the development environment', async () => {
    hoisted.currentEnvironmentId = 2;

    renderModeSelectionDialog();

    await userEvent.click(screen.getByRole('button', {name: 'Start with Automation'}));

    expect(hoisted.navigateMock).toHaveBeenCalledWith('/automation/deployments');
});
