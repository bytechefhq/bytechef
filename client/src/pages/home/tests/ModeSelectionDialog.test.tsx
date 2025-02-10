import {resetAll} from '@/pages/account/public/tests/utils/testUtils';
import ModeSelectionDialog from '@/pages/home/components/ModeSelectionDialog';
import {ModeType} from '@/pages/home/stores/useModeTypeStore';
import {mockModeTypeStore} from '@/pages/home/tests/mocks/mockModeTypeStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {Mock, afterEach, beforeEach, expect, it, vi} from 'vitest';

vi.mock('@/pages/home/stores/useModeTypeStore', async () => {
    const actual = await import('@/pages/home/stores/useModeTypeStore');
    return {
        ...actual,
        useModeTypeStore: vi.fn(),
    };
});

const mockedUseNavigate = vi.fn();

const mockReactRouter = () => {
    vi.mock('react-router-dom', async () => {
        const mod = await import('react-router-dom');
        return {
            ...mod,
            useNavigate: () => mockedUseNavigate,
        };
    });
};

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: vi.fn(),
}));

const mockedUseFeatureFlagsStore = useFeatureFlagsStore as Mock;

beforeEach(() => {
    mockReactRouter();
    mockModeTypeStore();
    mockedUseFeatureFlagsStore.mockReturnValue(() => true);
});

afterEach(() => {
    resetAll();
});

const mockOnClose = vi.fn();

const renderModeSelectionDialog = () => {
    render(
        <MemoryRouter>
            <Routes>
                <Route element={<ModeSelectionDialog handleDialogClose={mockOnClose} isDialogOpen={true} />} path="/" />
            </Routes>
        </MemoryRouter>
    );
};

it('should show dialog when isDialogOpen is true', () => {
    renderModeSelectionDialog();

    expect(screen.getByText('Select how you will use ByteChef')).toBeInTheDocument();
});

it('should not show dialog when isDialogOpen is false', () => {
    render(
        <MemoryRouter>
            <Routes>
                <Route
                    element={<ModeSelectionDialog handleDialogClose={mockOnClose} isDialogOpen={false} />}
                    path="/"
                />
            </Routes>
        </MemoryRouter>
    );

    expect(screen.queryByText('Select how you will use ByteChef')).not.toBeInTheDocument();
});

it('should show current type as checked on dialog open', () => {
    mockModeTypeStore({
        currentType: ModeType.EMBEDDED,
    });

    renderModeSelectionDialog();

    expect(screen.getByTestId('embedded')).toBeChecked();
});

it('should not show any type as checked if the current type is undefined', () => {
    mockModeTypeStore({
        currentType: undefined,
    });

    renderModeSelectionDialog();

    expect(screen.queryByTestId('embedded')).not.toBeChecked();
    expect(screen.queryByTestId('automation')).not.toBeChecked();
});

it('should disable "Confirm" button as default and enable it once the type is selected', async () => {
    renderModeSelectionDialog();

    expect(screen.getByText('Confirm')).toBeDisabled();

    await userEvent.click(screen.getByTestId('embedded'));

    expect(screen.getByText('Confirm')).toBeEnabled();
});

it('should show the cancel button and x icon', () => {
    renderModeSelectionDialog();

    expect(screen.getByLabelText('cancel')).toBeInTheDocument();

    expect(screen.getByText('Close')).toBeInTheDocument();
});

it('should not show the cancel button when currentType is undefined', () => {
    mockModeTypeStore({
        currentType: undefined,
    });

    renderModeSelectionDialog();

    expect(screen.queryByLabelText('cancel')).not.toBeInTheDocument();
});

it('should call the onDialogClose function on click of the "Cancel" button or x icon', async () => {
    renderModeSelectionDialog();

    await userEvent.click(screen.getByLabelText('cancel'));

    expect(mockOnClose).toHaveBeenCalledOnce();
});

it('should call the onDialogClose function on click of the "Confirm" button if it is enabled', async () => {
    renderModeSelectionDialog();

    await userEvent.click(screen.getByTestId('embedded'));

    await userEvent.click(screen.getByText('Confirm'));

    expect(mockOnClose).toHaveBeenCalledOnce();
});

it('should not call the onDialogClose function on click of escape key if the currentType is undefined', async () => {
    mockModeTypeStore({
        currentType: undefined,
    });

    renderModeSelectionDialog();

    await userEvent.keyboard('[Escape]');

    expect(mockOnClose).not.toHaveBeenCalled();
});

it('should call setCurrentType with appropriate value when a currentType is changed', async () => {
    const mockSetCurrentType = vi.fn();

    mockModeTypeStore({
        currentType: ModeType.EMBEDDED,
        setCurrentType: mockSetCurrentType,
    });

    renderModeSelectionDialog();

    await userEvent.click(screen.getByTestId('automation'));

    await userEvent.click(screen.getByText('Confirm'));

    expect(mockSetCurrentType).toHaveBeenCalledWith(ModeType.AUTOMATION);
});

it('should navigate to embedded page when currentType is changed to embedded', async () => {
    renderModeSelectionDialog();

    await userEvent.click(screen.getByTestId('embedded'));

    await userEvent.click(screen.getByText('Confirm'));

    expect(mockedUseNavigate).toHaveBeenCalledWith('/embedded');
});

it('should navigate to automation page when currentType is changed to automation', async () => {
    mockModeTypeStore({
        currentType: ModeType.EMBEDDED,
    });

    renderModeSelectionDialog();

    await userEvent.click(screen.getByTestId('automation'));

    await userEvent.click(screen.getByText('Confirm'));

    expect(mockedUseNavigate).toHaveBeenCalledWith('/automation');
});
