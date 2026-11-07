import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {currentWorkspaceIdRef, useGetAssetFilesQueryMock} = vi.hoisted(() => ({
    currentWorkspaceIdRef: {current: 3 as number | undefined},
    useGetAssetFilesQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useGetAssetFilesQuery: useGetAssetFilesQueryMock,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number | undefined}) => unknown) =>
        selector({currentWorkspaceId: currentWorkspaceIdRef.current}),
}));

vi.mock('use-debounce', () => ({
    useDebouncedCallback: (callback: (value: string) => void) => callback,
}));

const {default: AiHubFilePicker} = await import('../AiHubFilePicker');

const ASSET_FILES = [
    {id: '7', name: 'notes.md'},
    {id: '8', name: 'report.pdf'},
];

const renderPicker = () =>
    render(
        <TooltipProvider>
            <AiHubFilePicker />
        </TooltipProvider>
    );

describe('AiHubFilePicker', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        currentWorkspaceIdRef.current = 3;

        useGetAssetFilesQueryMock.mockReturnValue({data: {assetFiles: ASSET_FILES}, isLoading: false});

        aiHubComposerStore.setState({referencedResources: [], resourcePickerOpen: false});
        aiHubTabsStore.getState().reset();
    });

    it('fetches the workspace files only once it is open', async () => {
        renderPicker();

        expect(useGetAssetFilesQueryMock).toHaveBeenLastCalledWith(expect.objectContaining({workspaceId: '3'}), {
            enabled: false,
        });

        await userEvent.click(screen.getByRole('button', {name: 'Open file'}));

        expect(useGetAssetFilesQueryMock).toHaveBeenLastCalledWith(expect.objectContaining({workspaceId: '3'}), {
            enabled: true,
        });
    });

    it('does not fetch files for an unknown workspace', async () => {
        currentWorkspaceIdRef.current = undefined;

        renderPicker();

        await userEvent.click(screen.getByRole('button', {name: 'Open file'}));

        expect(useGetAssetFilesQueryMock).toHaveBeenLastCalledWith(expect.anything(), {enabled: false});
    });

    it('opens a picked file as a tab and attaches it to the composer', async () => {
        renderPicker();

        await userEvent.click(screen.getByRole('button', {name: 'Open file'}));
        await userEvent.click(await screen.findByText('report.pdf'));

        const tabsState = aiHubTabsStore.getState();

        expect(tabsState.openTabs).toHaveLength(1);
        expect(tabsState.openTabs[0]).toMatchObject({fileId: '8', kind: 'file', name: 'report.pdf'});
        expect(aiHubComposerStore.getState().referencedResources).toEqual([
            {id: '8', kind: 'file', name: 'report.pdf', ownsTab: true, tabId: tabsState.openTabs[0]!.id},
        ]);
    });

    it('filters files by name', async () => {
        renderPicker();

        await userEvent.click(screen.getByRole('button', {name: 'Open file'}));
        await userEvent.type(screen.getByPlaceholderText('Search files…'), 'notes');

        await waitFor(() => {
            expect(screen.queryByText('report.pdf')).not.toBeInTheDocument();
        });

        expect(screen.getByText('notes.md')).toBeInTheDocument();
    });

    it('says when the workspace has no files', async () => {
        useGetAssetFilesQueryMock.mockReturnValue({data: {assetFiles: []}, isLoading: false});

        renderPicker();

        await userEvent.click(screen.getByRole('button', {name: 'Open file'}));

        expect(await screen.findByText('No files.')).toBeInTheDocument();
    });
});
