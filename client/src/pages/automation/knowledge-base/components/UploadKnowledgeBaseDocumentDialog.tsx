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
import {Label} from '@/components/ui/label';
import useUploadKnowledgeBaseDocumentDialog from '@/pages/automation/knowledge-base/components/hooks/useUploadKnowledgeBaseDocumentDialog';
import {cn} from '@/shared/util/cn-utils';
import {Loader2, Upload, X} from 'lucide-react';
import {ReactNode} from 'react';

interface UploadKnowledgeBaseDocumentDialogProps {
    knowledgeBaseId: string;
    trigger?: ReactNode;
}

const UploadKnowledgeBaseDocumentDialog = ({knowledgeBaseId, trigger}: UploadKnowledgeBaseDocumentDialogProps) => {
    const {
        canSubmit,
        formatFileSize,
        handleFileChange,
        handleOpenChange,
        handleSubmit,
        open,
        removeFile,
        selectedFiles,
        setOpen,
        uploading,
    } = useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId});

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogTrigger asChild>
                {trigger ?? (
                    <Button size="sm">
                        <Upload className="mr-2 size-4" />
                        Upload Document
                    </Button>
                )}
            </DialogTrigger>

            <DialogContent className="sm:max-w-[600px]">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Upload Documents</DialogTitle>

                        <DialogDescription>
                            Upload documents to be processed and indexed in the knowledge base.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label>Select Files</Label>

                        <div
                            className={cn(
                                'flex flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 p-6',
                                uploading ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-gray-50'
                            )}
                            onClick={() => !uploading && document.getElementById('document-file-upload')?.click()}
                        >
                            <Upload className="mb-2 size-8 text-gray-400" />

                            <p className="text-sm text-gray-600">Drop files here or click to browse</p>

                            <p className="mt-1 text-xs text-gray-400">
                                PDF, DOC, DOCX, TXT, CSV, XLS, XLSX, MD, PPT, PPTX, HTML (max 100MB each)
                            </p>

                            <input
                                className="hidden"
                                disabled={uploading}
                                id="document-file-upload"
                                multiple
                                onChange={handleFileChange}
                                type="file"
                            />
                        </div>
                    </div>

                    {selectedFiles.length > 0 && (
                        <div className="space-y-2">
                            <Label>Selected Files ({selectedFiles.length})</Label>

                            <div className="max-h-[250px] space-y-2 overflow-y-auto">
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
                                            {file.status === 'uploading' && (
                                                <div className="flex items-center space-x-2">
                                                    <span className="text-xs text-gray-400">Uploading...</span>

                                                    <Loader2 className="size-4 animate-spin text-gray-400" />
                                                </div>
                                            )}

                                            {file.status === 'completed' && (
                                                <div className="flex size-4 items-center justify-center rounded-full bg-green-500">
                                                    <svg
                                                        className="size-3 text-white"
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
                                                    onClick={(event) => {
                                                        event.stopPropagation();
                                                        removeFile(idx);
                                                    }}
                                                    type="button"
                                                >
                                                    <X className="size-4 text-gray-500" />
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

                    <Button disabled={!canSubmit || uploading} onClick={handleSubmit}>
                        {uploading
                            ? `Uploading ${selectedFiles.filter((f) => f.status === 'completed').length}/${selectedFiles.length}...`
                            : 'Upload'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default UploadKnowledgeBaseDocumentDialog;
