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
import useUploadKnowledgeBaseDocumentDialog from '@/pages/automation/knowledge-base/components/hooks/useUploadKnowledgeBaseDocumentDialog';
import {Loader2Icon, UploadIcon, XIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

const SELECTED_FILES_MAX_HEIGHT = 250;

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
        uploading,
    } = useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId});

    const defaultTrigger = (
        <Button size="sm">
            <UploadIcon className="mr-2 size-4" />
            Upload Document
        </Button>
    );

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogTrigger asChild>{trigger || defaultTrigger}</DialogTrigger>

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
                    <fieldset className="space-y-2 border-0">
                        <label className="text-sm font-medium" htmlFor="document-file-upload">
                            Select Files
                        </label>

                        <div
                            className={twMerge(
                                'flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 p-6 hover:bg-gray-50',
                                uploading && 'cursor-not-allowed opacity-50'
                            )}
                        >
                            <label className="flex cursor-pointer flex-col items-center" htmlFor="document-file-upload">
                                <UploadIcon className="mb-2 size-8 text-gray-400" />

                                <span className="text-sm text-gray-600">Click to browse files</span>

                                <span className="mt-1 text-xs text-gray-400">
                                    Supported: PDF, DOC, DOCX, TXT, CSV, XLS, XLSX, MD, PPT, PPTX, HTML
                                </span>
                            </label>

                            <input
                                accept=".pdf,.doc,.docx,.txt,.csv,.xls,.xlsx,.md,.ppt,.pptx,.html,.htm"
                                className="hidden"
                                disabled={uploading}
                                id="document-file-upload"
                                multiple
                                onChange={handleFileChange}
                                type="file"
                            />
                        </div>
                    </fieldset>

                    {selectedFiles.length > 0 && (
                        <fieldset className="space-y-2 border-0">
                            <span className="text-sm font-medium">Selected Files ({selectedFiles.length})</span>

                            <div
                                className="space-y-2 overflow-y-auto"
                                style={{maxHeight: `${SELECTED_FILES_MAX_HEIGHT}px`}}
                            >
                                {selectedFiles.map((selectedFile, selectedFileIndex) => (
                                    <div
                                        className="flex items-center justify-between rounded-md border border-gray-200 bg-gray-50 p-2 px-3"
                                        key={`${selectedFile.file.name}-${selectedFile.file.size}-${selectedFile.file.lastModified}`}
                                    >
                                        <div className="flex flex-1 items-center space-x-2 overflow-hidden">
                                            <span className="truncate text-sm font-medium">
                                                {selectedFile.file.name}
                                            </span>

                                            <span className="text-xs text-gray-400">
                                                {formatFileSize(selectedFile.file.size)}
                                            </span>
                                        </div>

                                        <div className="ml-2 flex items-center space-x-2">
                                            {selectedFile.status === 'uploading' && (
                                                <div className="flex items-center space-x-2">
                                                    <span className="text-xs text-gray-400">Uploading...</span>

                                                    <Loader2Icon className="size-4 animate-spin text-gray-400" />
                                                </div>
                                            )}

                                            {selectedFile.status === 'completed' && (
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

                                            {selectedFile.status === 'error' && (
                                                <div className="flex items-center space-x-2">
                                                    <span className="text-xs text-red-500">
                                                        {selectedFile.statusMessage || 'Error'}
                                                    </span>
                                                </div>
                                            )}

                                            {!uploading && (
                                                <Button
                                                    aria-label={`Remove ${selectedFile.file.name}`}
                                                    className="rounded-full hover:bg-gray-200"
                                                    icon={<XIcon className="size-4 text-gray-500" />}
                                                    onClick={(event) => {
                                                        event.stopPropagation();
                                                        removeFile(selectedFileIndex);
                                                    }}
                                                    size="iconXs"
                                                    variant="ghost"
                                                />
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </fieldset>
                    )}
                </div>

                <DialogFooter>
                    <Button disabled={uploading} onClick={() => handleOpenChange(false)} variant="ghost">
                        Cancel
                    </Button>

                    <Button disabled={!canSubmit || uploading} onClick={handleSubmit}>
                        {uploading
                            ? `Uploading ${selectedFiles.filter((selectedFile) => selectedFile.status === 'completed').length}/${selectedFiles.length}...`
                            : 'Upload'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default UploadKnowledgeBaseDocumentDialog;
