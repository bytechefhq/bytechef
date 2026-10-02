import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
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
        acceptedExtensions,
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

    const acceptedExtensionsLabel = acceptedExtensions
        .split(',')
        .map((extension) => extension.replace('.', '').toUpperCase())
        .join(', ');

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogTrigger asChild>
                {trigger ?? <Button icon={<UploadIcon />} label="Import Component" />}
            </DialogTrigger>

            <DialogContent size="md">
                <DialogMain>
                    <DialogHeader
                        description="Upload a custom component file to deploy it to the platform."
                        title="Import Custom Component"
                    />

                    <DialogBody>
                        <div className="space-y-4">
                            <div className="space-y-2">
                                <Label>Select Component File</Label>

                                <div
                                    className={twMerge(
                                        'flex flex-col items-center justify-center rounded-lg border-2 border-dashed border-stroke-neutral-tertiary p-6',
                                        uploading
                                            ? 'cursor-not-allowed opacity-50'
                                            : 'cursor-pointer hover:bg-surface-neutral-primary-hover'
                                    )}
                                    onClick={() =>
                                        !uploading && document.getElementById('component-file-upload')?.click()
                                    }
                                >
                                    <UploadIcon className="mb-2 size-8 text-content-neutral-tertiary" />

                                    <p className="text-sm text-content-neutral-secondary">
                                        Drop files here or click to browse
                                    </p>

                                    <p className="mt-1 text-xs text-content-neutral-tertiary">
                                        {acceptedExtensionsLabel} files
                                    </p>

                                    <input
                                        accept={acceptedExtensions}
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
                                                className="flex items-center justify-between rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary p-2 px-3"
                                                key={index}
                                            >
                                                <div className="flex flex-1 items-center space-x-2 overflow-hidden">
                                                    <span className="truncate text-sm font-medium">
                                                        {file.file.name}
                                                    </span>

                                                    <span className="text-xs text-content-neutral-tertiary">
                                                        {formatFileSize(file.file.size)}
                                                    </span>
                                                </div>

                                                <div className="ml-2 flex items-center space-x-2">
                                                    {file.status === 'uploading' && (
                                                        <div className="flex items-center space-x-2">
                                                            <span className="text-xs text-content-neutral-tertiary">
                                                                Uploading...
                                                            </span>

                                                            <Loader2Icon className="size-4 animate-spin text-content-neutral-tertiary" />
                                                        </div>
                                                    )}

                                                    {file.status === 'completed' && (
                                                        <div className="flex size-4 items-center justify-center rounded-full bg-green-500">
                                                            <svg
                                                                className="size-3 text-content-onsurface-primary"
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
                                                            <span className="text-xs text-content-destructive">
                                                                {file.statusMessage || 'Error'}
                                                            </span>
                                                        </div>
                                                    )}

                                                    {!uploading && (
                                                        <button
                                                            className="rounded-full p-1 hover:bg-surface-neutral-secondary-hover"
                                                            onClick={(event) => {
                                                                event.stopPropagation();
                                                                removeFile(index);
                                                            }}
                                                            type="button"
                                                        >
                                                            <XIcon className="size-4 text-content-neutral-secondary" />
                                                        </button>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <Button disabled={uploading} label="Cancel" onClick={() => setOpen(false)} variant="outline" />

                        <Button
                            disabled={!canSubmit || uploading}
                            label={
                                uploading
                                    ? `Uploading ${selectedFiles.filter((selectedFile) => selectedFile.status === 'completed').length}/${selectedFiles.length}...`
                                    : 'Import'
                            }
                            onClick={handleSubmit}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default UploadCustomComponentDialog;
