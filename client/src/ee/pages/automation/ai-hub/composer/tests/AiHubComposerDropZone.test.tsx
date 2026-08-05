import AiHubComposerDropZone from '@/ee/pages/automation/ai-hub/composer/AiHubComposerDropZone';
import {fireEvent, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, test, vi} from 'vitest';

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

const {toast} = await import('sonner');

const mockToast = vi.mocked(toast);

const buildDataTransfer = (files: File[]): DataTransfer => {
    const types = files.length > 0 ? ['Files'] : [];

    return {
        dropEffect: 'none',
        files: files as unknown as FileList,
        items: [] as unknown as DataTransferItemList,
        types,
    } as unknown as DataTransfer;
};

describe('AiHubComposerDropZone', () => {
    beforeEach(() => {
        mockToast.error.mockReset();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    test('renders children inside the zone', () => {
        render(
            <AiHubComposerDropZone onFilesDropped={vi.fn()}>
                <div>composer</div>
            </AiHubComposerDropZone>
        );

        expect(screen.getByText('composer')).toBeInTheDocument();
    });

    test('shows the drop overlay during a Files drag', () => {
        render(
            <AiHubComposerDropZone onFilesDropped={vi.fn()}>
                <div>composer</div>
            </AiHubComposerDropZone>
        );

        const zone = screen.getByTestId('ai-hub-composer-drop-zone');

        fireEvent.dragEnter(zone, {dataTransfer: buildDataTransfer([new File(['x'], 'a.png', {type: 'image/png'})])});

        expect(screen.getByTestId('ai-hub-composer-drop-overlay')).toBeInTheDocument();
    });

    test('hides the overlay when drag leaves', () => {
        render(
            <AiHubComposerDropZone onFilesDropped={vi.fn()}>
                <div>composer</div>
            </AiHubComposerDropZone>
        );

        const zone = screen.getByTestId('ai-hub-composer-drop-zone');

        fireEvent.dragEnter(zone, {dataTransfer: buildDataTransfer([new File(['x'], 'a.png', {type: 'image/png'})])});

        expect(screen.getByTestId('ai-hub-composer-drop-overlay')).toBeInTheDocument();

        fireEvent.dragLeave(zone, {dataTransfer: buildDataTransfer([])});

        expect(screen.queryByTestId('ai-hub-composer-drop-overlay')).not.toBeInTheDocument();
    });

    test('forwards allowed dropped files to the callback', () => {
        const onFilesDropped = vi.fn();
        const file = new File(['data'], 'shot.png', {type: 'image/png'});

        render(
            <AiHubComposerDropZone onFilesDropped={onFilesDropped}>
                <div>composer</div>
            </AiHubComposerDropZone>
        );

        const zone = screen.getByTestId('ai-hub-composer-drop-zone');

        fireEvent.drop(zone, {dataTransfer: buildDataTransfer([file])});

        expect(onFilesDropped).toHaveBeenCalledWith([file]);
    });

    test('rejects unsupported mime types via toast and does not call the callback', () => {
        const onFilesDropped = vi.fn();
        const file = new File(['data'], 'archive.zip', {type: 'application/zip'});

        render(
            <AiHubComposerDropZone onFilesDropped={onFilesDropped}>
                <div>composer</div>
            </AiHubComposerDropZone>
        );

        const zone = screen.getByTestId('ai-hub-composer-drop-zone');

        fireEvent.drop(zone, {dataTransfer: buildDataTransfer([file])});

        expect(mockToast.error).toHaveBeenCalledWith(expect.stringMatching(/File type not supported/));
        expect(onFilesDropped).not.toHaveBeenCalled();
    });

    test('partial drops only forward the allowed subset', () => {
        const onFilesDropped = vi.fn();
        const allowedFile = new File(['data'], 'good.pdf', {type: 'application/pdf'});
        const rejectedFile = new File(['data'], 'bad.zip', {type: 'application/zip'});

        render(
            <AiHubComposerDropZone onFilesDropped={onFilesDropped}>
                <div>composer</div>
            </AiHubComposerDropZone>
        );

        const zone = screen.getByTestId('ai-hub-composer-drop-zone');

        fireEvent.drop(zone, {dataTransfer: buildDataTransfer([allowedFile, rejectedFile])});

        expect(onFilesDropped).toHaveBeenCalledWith([allowedFile]);
        expect(mockToast.error).toHaveBeenCalledWith(expect.stringContaining('bad.zip'));
    });

    test('disabled zone does not show the drag overlay (workflow-chat tasks)', () => {
        render(
            <AiHubComposerDropZone disabled onFilesDropped={vi.fn()}>
                <div>composer</div>
            </AiHubComposerDropZone>
        );

        const zone = screen.getByTestId('ai-hub-composer-drop-zone');

        fireEvent.dragEnter(zone, {dataTransfer: buildDataTransfer([new File(['x'], 'a.png', {type: 'image/png'})])});

        expect(screen.queryByTestId('ai-hub-composer-drop-overlay')).not.toBeInTheDocument();
    });

    test('disabled zone ignores dropped files', () => {
        const onFilesDropped = vi.fn();
        const file = new File(['data'], 'shot.png', {type: 'image/png'});

        render(
            <AiHubComposerDropZone disabled onFilesDropped={onFilesDropped}>
                <div>composer</div>
            </AiHubComposerDropZone>
        );

        const zone = screen.getByTestId('ai-hub-composer-drop-zone');

        fireEvent.drop(zone, {dataTransfer: buildDataTransfer([file])});

        expect(onFilesDropped).not.toHaveBeenCalled();
        expect(mockToast.error).not.toHaveBeenCalled();
    });
});
