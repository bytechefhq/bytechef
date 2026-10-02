import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import UploadKnowledgeBaseDocumentDialog from '../UploadKnowledgeBaseDocumentDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleFileChange: vi.fn(),
        handleOpenChange: vi.fn(),
        handleSubmit: vi.fn(),
        mockUseUploadKnowledgeBaseDocumentDialog: vi.fn(),
        removeFile: vi.fn(),
        setOpen: vi.fn(),
    };
});

vi.mock('../hooks/useUploadKnowledgeBaseDocumentDialog', () => ({
    default: hoisted.mockUseUploadKnowledgeBaseDocumentDialog,
}));

const defaultMockReturn = {
    canSubmit: true,
    formatFileSize: (bytes: number) => `${bytes} bytes`,
    handleFileChange: hoisted.handleFileChange,
    handleOpenChange: hoisted.handleOpenChange,
    handleSubmit: hoisted.handleSubmit,
    open: true,
    removeFile: hoisted.removeFile,
    selectedFiles: [] as {
        file: File;
        status: 'completed' | 'error' | 'pending' | 'uploading';
        statusMessage?: string;
    }[],
    setOpen: hoisted.setOpen,
    uploading: false,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseUploadKnowledgeBaseDocumentDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('UploadKnowledgeBaseDocumentDialog', () => {
    it('renders dialog when open', () => {
        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('renders dialog title', () => {
        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByRole('heading', {name: 'Upload Documents'})).toBeInTheDocument();
    });

    it('renders dialog description', () => {
        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(
            screen.getByText('Upload documents to be processed and indexed in the knowledge base.')
        ).toBeInTheDocument();
    });

    it('renders file drop zone', () => {
        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Click to browse files')).toBeInTheDocument();
    });

    it('renders supported file types', () => {
        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByText(/PDF, DOC, DOCX, TXT/)).toBeInTheDocument();
    });

    it('renders Upload button', () => {
        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Upload')).toBeInTheDocument();
    });

    it('disables Upload button when canSubmit is false', () => {
        hoisted.mockUseUploadKnowledgeBaseDocumentDialog.mockReturnValue({
            ...defaultMockReturn,
            canSubmit: false,
        });

        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        const uploadButton = screen.getByText('Upload').closest('button');

        expect(uploadButton).toBeDisabled();
    });

    it('shows uploading progress text', () => {
        hoisted.mockUseUploadKnowledgeBaseDocumentDialog.mockReturnValue({
            ...defaultMockReturn,
            selectedFiles: [
                {file: new File([''], 'test1.pdf'), status: 'completed'},
                {file: new File([''], 'test2.pdf'), status: 'uploading'},
            ],
            uploading: true,
        });

        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Uploading 1/2...')).toBeInTheDocument();
    });

    it('renders selected files', () => {
        hoisted.mockUseUploadKnowledgeBaseDocumentDialog.mockReturnValue({
            ...defaultMockReturn,
            selectedFiles: [{file: new File(['test'], 'test.pdf', {type: 'application/pdf'}), status: 'pending'}],
        });

        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByText('test.pdf')).toBeInTheDocument();
    });

    it('shows Selected Files count', () => {
        hoisted.mockUseUploadKnowledgeBaseDocumentDialog.mockReturnValue({
            ...defaultMockReturn,
            selectedFiles: [
                {file: new File([''], 'test1.pdf'), status: 'pending'},
                {file: new File([''], 'test2.pdf'), status: 'pending'},
            ],
        });

        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Selected Files (2)')).toBeInTheDocument();
    });

    it('shows uploading status for uploading files', () => {
        hoisted.mockUseUploadKnowledgeBaseDocumentDialog.mockReturnValue({
            ...defaultMockReturn,
            selectedFiles: [{file: new File([''], 'test.pdf'), status: 'uploading'}],
            uploading: true,
        });

        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Uploading...')).toBeInTheDocument();
    });

    it('shows error status for failed files', () => {
        hoisted.mockUseUploadKnowledgeBaseDocumentDialog.mockReturnValue({
            ...defaultMockReturn,
            selectedFiles: [{file: new File([''], 'test.pdf'), status: 'error', statusMessage: 'Upload failed'}],
        });

        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Upload failed')).toBeInTheDocument();
    });

    it('calls handleSubmit when Upload is clicked', async () => {
        render(<UploadKnowledgeBaseDocumentDialog knowledgeBaseId="kb-1" />);

        const uploadButton = screen.getByText('Upload');
        await userEvent.click(uploadButton);

        expect(hoisted.handleSubmit).toHaveBeenCalled();
    });
});
