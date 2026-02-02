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
import useUploadCustomComponentDialog from '@/ee/pages/settings/platform/custom-components/components/hooks/useUploadCustomComponentDialog';
import {Loader2Icon, UploadIcon, XIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface UploadCustomComponentDialogProps {
    trigger?: ReactNode;
}

const UploadCustomComponentDialog = ({trigger}: UploadCustomComponentDialogProps) => {
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
    } = useUploadCustomComponentDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogTrigger asChild>
                {trigger ?? (
                    <Button>
                        <UploadIcon className="mr-2 size-4" />
                        Import Component
                    </Button>
                )}
            </DialogTrigger>

            <DialogContent className="sm:max-w-[600px]">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Import Custom Component</DialogTitle>

                        <DialogDescription>
                            Upload a custom component JAR file to deploy it to the platform.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label>Select Component File</Label>

                        <div
                            className={twMerge(
                                'flex flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 p-6',
                                uploading ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-gray-50'
                            )}
                            onClick={() => !uploading && document.getElementById('component-file-upload')?.click()}
                        >
                            <UploadIcon className="mb-2 size-8 text-gray-400" />

                            <p className="text-sm text-gray-600">Drop files here or click to browse</p>

                            <p className="mt-1 text-xs text-gray-400">.JAR, .JS, .PY, .RB files</p>

                            <input
                                accept=".jar,.js,.py,.rb"
                                className="hidden"
                                disabled={uploading}
                                id="component-file-upload"
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
                                {selectedFiles.map((file, index) => (
                                    <div
                                        className="flex items-center justify-between rounded-md border border-gray-200 bg-gray-50 p-2 px-3"
                                        key={index}
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

                                                    <Loader2Icon className="size-4 animate-spin text-gray-400" />
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
                                                        removeFile(index);
                                                    }}
                                                    type="button"
                                                >
                                                    <XIcon className="size-4 text-gray-500" />
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
                            ? `Uploading ${selectedFiles.filter((selectedFile) => selectedFile.status === 'completed').length}/${selectedFiles.length}...`
                            : 'Import'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default UploadCustomComponentDialog;
