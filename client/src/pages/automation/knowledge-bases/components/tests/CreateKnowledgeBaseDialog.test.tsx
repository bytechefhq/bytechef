import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CreateKnowledgeBaseDialog from '../CreateKnowledgeBaseDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleFileChange: vi.fn(),
        handleOpenChange: vi.fn(),
        handleSubmit: vi.fn(),
        mockUseCreateKnowledgeBaseDialog: vi.fn(),
        removeFile: vi.fn(),
        setDescription: vi.fn(),
        setMaxChunkSize: vi.fn(),
        setMinChunkSizeChars: vi.fn(),
        setName: vi.fn(),
        setOpen: vi.fn(),
        setOverlapSize: vi.fn(),
    };
});

vi.mock('../hooks/useCreateKnowledgeBaseDialog', () => ({
    default: hoisted.mockUseCreateKnowledgeBaseDialog,
}));

vi.mock('@/components/Button/Button', () => ({
    default: ({
        children,
        disabled,
        onClick,
    }: {
        children?: React.ReactNode;
        disabled?: boolean;
        onClick?: () => void;
        variant?: string;
    }) => (
        <button data-testid="button" disabled={disabled} onClick={onClick}>
            {children}
        </button>
    ),
}));

vi.mock('@/components/ui/dialog', () => ({
    Dialog: ({children, open}: {children: React.ReactNode; onOpenChange?: (open: boolean) => void; open?: boolean}) =>
        open ? <div data-testid="dialog">{children}</div> : null,
    DialogCloseButton: () => <button data-testid="dialog-close">Close</button>,
    DialogContent: ({children}: {children: React.ReactNode; className?: string}) => (
        <div data-testid="dialog-content">{children}</div>
    ),
    DialogDescription: ({children}: {children: React.ReactNode}) => <p data-testid="dialog-description">{children}</p>,
    DialogFooter: ({children}: {children: React.ReactNode}) => <div data-testid="dialog-footer">{children}</div>,
    DialogHeader: ({children}: {children: React.ReactNode; className?: string}) => (
        <div data-testid="dialog-header">{children}</div>
    ),
    DialogTitle: ({children}: {children: React.ReactNode}) => <h2 data-testid="dialog-title">{children}</h2>,
    DialogTrigger: ({children}: {asChild?: boolean; children: React.ReactNode}) => (
        <div data-testid="dialog-trigger">{children}</div>
    ),
}));

const defaultMockReturn = {
    canSubmit: true,
    createMutation: {isPending: false},
    description: '',
    formatFileSize: (bytes: number) => `${bytes} bytes`,
    handleFileChange: hoisted.handleFileChange,
    handleOpenChange: hoisted.handleOpenChange,
    handleSubmit: hoisted.handleSubmit,
    maxChunkSize: '1024',
    minChunkSizeChars: '1',
    name: '',
    open: true,
    overlapSize: '200',
    removeFile: hoisted.removeFile,
    selectedFiles: [] as {file: File; status: 'completed' | 'error' | 'pending' | 'processing' | 'uploading'}[],
    setDescription: hoisted.setDescription,
    setMaxChunkSize: hoisted.setMaxChunkSize,
    setMinChunkSizeChars: hoisted.setMinChunkSizeChars,
    setName: hoisted.setName,
    setOpen: hoisted.setOpen,
    setOverlapSize: hoisted.setOverlapSize,
    uploading: false,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseCreateKnowledgeBaseDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('CreateKnowledgeBaseDialog', () => {
    it('renders dialog when open', () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByTestId('dialog')).toBeInTheDocument();
    });

    it('renders dialog title', () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByTestId('dialog-title')).toHaveTextContent('Create Knowledge Base');
    });

    it('renders name input', () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByPlaceholderText('New KB')).toBeInTheDocument();
    });

    it('renders description textarea', () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByPlaceholderText('Describe this knowledge base (optional)')).toBeInTheDocument();
    });

    it('renders min chunk size input', () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByText('Min Chunk Size (characters)')).toBeInTheDocument();
    });

    it('renders max chunk size input', () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByText('Max Chunk Size (tokens)')).toBeInTheDocument();
    });

    it('renders overlap size input', () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByText('Overlap Size (tokens)')).toBeInTheDocument();
    });

    it('renders file upload area', () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByText('Drop files here or click to browse')).toBeInTheDocument();
    });

    it('calls setName when name input changes', async () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        const nameInput = screen.getByPlaceholderText('New KB');
        await userEvent.type(nameInput, 'Test');

        expect(hoisted.setName).toHaveBeenCalled();
    });

    it('calls setDescription when description changes', async () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        const descriptionInput = screen.getByPlaceholderText('Describe this knowledge base (optional)');
        await userEvent.type(descriptionInput, 'Test');

        expect(hoisted.setDescription).toHaveBeenCalled();
    });

    it('disables Create button when canSubmit is false', () => {
        hoisted.mockUseCreateKnowledgeBaseDialog.mockReturnValue({
            ...defaultMockReturn,
            canSubmit: false,
        });

        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        const createButton = screen.getByText('Create').closest('button');

        expect(createButton).toBeDisabled();
    });

    it('shows Creating... when mutation is pending', () => {
        hoisted.mockUseCreateKnowledgeBaseDialog.mockReturnValue({
            ...defaultMockReturn,
            createMutation: {isPending: true},
        });

        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByText('Creating...')).toBeInTheDocument();
    });

    it('shows upload progress when uploading', () => {
        hoisted.mockUseCreateKnowledgeBaseDialog.mockReturnValue({
            ...defaultMockReturn,
            selectedFiles: [
                {file: new File([''], 'test.pdf'), status: 'completed'},
                {file: new File([''], 'test2.pdf'), status: 'uploading'},
            ],
            uploading: true,
        });

        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByText('Uploading 1/2...')).toBeInTheDocument();
    });

    it('renders selected files', () => {
        hoisted.mockUseCreateKnowledgeBaseDialog.mockReturnValue({
            ...defaultMockReturn,
            selectedFiles: [{file: new File([''], 'test.pdf'), status: 'pending'}],
        });

        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        expect(screen.getByText('test.pdf')).toBeInTheDocument();
    });

    it('calls handleSubmit when Create is clicked', async () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-1" />);

        const createButton = screen.getByText('Create');
        await userEvent.click(createButton);

        expect(hoisted.handleSubmit).toHaveBeenCalled();
    });

    it('passes workspaceId to hook', () => {
        render(<CreateKnowledgeBaseDialog workspaceId="ws-123" />);

        expect(hoisted.mockUseCreateKnowledgeBaseDialog).toHaveBeenCalledWith({workspaceId: 'ws-123'});
    });
});
