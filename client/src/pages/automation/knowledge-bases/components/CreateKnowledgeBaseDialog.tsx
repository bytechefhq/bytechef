import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useCreateKnowledgeBaseDialog from '@/pages/automation/knowledge-bases/components/hooks/useCreateKnowledgeBaseDialog';
import {cn} from '@/shared/util/cn-utils';
import {Loader2, X} from 'lucide-react';
import {ReactNode} from 'react';

interface CreateKnowledgeBaseDialogProps {
    trigger?: ReactNode;
    workspaceId: string;
}

const CreateKnowledgeBaseDialog = ({trigger, workspaceId}: CreateKnowledgeBaseDialogProps) => {
    const {
        canSubmit,
        createMutation,
        description,
        formatFileSize,
        handleFileChange,
        handleOpenChange,
        handleSubmit,
        maxChunkSize,
        minChunkSizeChars,
        name,
        open,
        overlapSize,
        removeFile,
        selectedFiles,
        setDescription,
        setMaxChunkSize,
        setMinChunkSizeChars,
        setName,
        setOpen,
        setOverlapSize,
        uploading,
    } = useCreateKnowledgeBaseDialog({workspaceId});

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogTrigger asChild>{trigger ?? <Button>Create Knowledge Base</Button>}</DialogTrigger>

            <DialogContent className="sm:max-w-[600px]">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Create Knowledge Base</DialogTitle>

                        <DialogDescription>
                            Configure chunking settings and optionally upload initial documents.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label htmlFor="name">Name</Label>

                        <Input
                            disabled={uploading}
                            id="name"
                            onChange={(e) => setName(e.target.value)}
                            placeholder="New KB"
                            value={name}
                        />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="description">Description</Label>

                        <Textarea
                            disabled={uploading}
                            id="description"
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="Describe this knowledge base (optional)"
                            value={description}
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="minChunkSizeChars">Min Chunk Size (characters)</Label>

                            <Input
                                disabled={uploading}
                                id="minChunkSizeChars"
                                onChange={(e) => setMinChunkSizeChars(e.target.value)}
                                type="number"
                                value={minChunkSizeChars}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="maxChunkSize">Max Chunk Size (tokens)</Label>

                            <Input
                                disabled={uploading}
                                id="maxChunkSize"
                                onChange={(e) => setMaxChunkSize(e.target.value)}
                                type="number"
                                value={maxChunkSize}
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="overlapSize">Overlap Size (tokens)</Label>

                        <Input
                            disabled={uploading}
                            id="overlapSize"
                            onChange={(e) => setOverlapSize(e.target.value)}
                            type="number"
                            value={overlapSize}
                        />
                    </div>

                    <div className="space-y-2">
                        <Label>Upload Documents</Label>

                        <div
                            className={cn(
                                'flex flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 p-6',
                                uploading ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-gray-50'
                            )}
                            onClick={() => !uploading && document.getElementById('file-upload')?.click()}
                        >
                            <p className="text-sm text-gray-600">Drop files here or click to browse</p>

                            <p className="mt-1 text-xs text-gray-400">
                                Supported: PDF, DOC, DOCX, TXT, CSV, XLS, XLSX, MD, PPT, PPTX, HTML
                            </p>

                            <input
                                accept=".pdf,.doc,.docx,.txt,.csv,.xls,.xlsx,.md,.ppt,.pptx,.html,.htm"
                                className="hidden"
                                disabled={uploading}
                                id="file-upload"
                                multiple
                                onChange={handleFileChange}
                                type="file"
                            />
                        </div>
                    </div>

                    {selectedFiles.length > 0 && (
                        <div className="space-y-2">
                            <Label>Selected Files</Label>

                            <div className="max-h-[200px] space-y-2 overflow-y-auto">
                                {selectedFiles.map((file, idx) => (
                                    <div
                                        className="flex items-center justify-between rounded-md border border-gray-200 bg-gray-50 p-2 px-3"
                                        key={idx}
                                    >
                                        <div className="flex flex-1 items-center space-x-2 overflow-hidden">
                                            <span className="truncate text-sm font-medium">{file.file.name}</span>

                                            <span className="text-xs text-gray-400">
                                                {formatFileSize(file.file.size)}
                                            </span>
                                        </div>

                                        <div className="ml-2 flex items-center space-x-2">
                                            {(file.status === 'uploading' || file.status === 'processing') && (
                                                <div className="flex items-center space-x-2">
                                                    <span className="text-xs text-gray-400">
                                                        {file.status === 'uploading' ? 'Uploading...' : 'Processing...'}
                                                    </span>

                                                    <Loader2 className="h-4 w-4 animate-spin text-gray-400" />
                                                </div>
                                            )}

                                            {file.status === 'completed' && (
                                                <div className="flex h-4 w-4 items-center justify-center rounded-full bg-green-500">
                                                    <svg
                                                        className="h-3 w-3 text-white"
                                                        fill="none"
                                                        stroke="currentColor"
                                                        viewBox="0 0 24 24"
                                                    >
                                                        <path
                                                            d="M5 13l4 4L19 7"
                                                            strokeLinecap="round"
                                                            strokeLinejoin="round"
                                                            strokeWidth="2"
                                                        />
                                                    </svg>
                                                </div>
                                            )}

                                            {file.status === 'error' && (
                                                <div className="flex items-center space-x-2">
                                                    <span className="text-xs text-red-500">
                                                        {file.statusMessage || 'Error'}
                                                    </span>
                                                </div>
                                            )}

                                            {!uploading && (
                                                <button
                                                    className="rounded-full p-1 hover:bg-gray-200"
                                                    onClick={() => removeFile(idx)}
                                                >
                                                    <X className="h-4 w-4 text-gray-500" />
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>

                <DialogFooter>
                    <Button disabled={uploading} onClick={() => setOpen(false)} variant="ghost">
                        Cancel
                    </Button>

                    <Button disabled={!canSubmit || createMutation.isPending || uploading} onClick={handleSubmit}>
                        {uploading
                            ? `Uploading ${selectedFiles.filter((file) => file.status === 'completed').length}/${selectedFiles.length}...`
                            : createMutation.isPending
                              ? 'Creating...'
                              : 'Create'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default CreateKnowledgeBaseDialog;
