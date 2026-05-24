import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import useAiSkillUploadForm from '@/pages/automation/ai/skills/hooks/useAiSkillUploadForm';
import {FileIcon, UploadIcon, XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface AiSkillUploadDialogProps {
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const AiSkillUploadDialog = ({onOpenChange, open}: AiSkillUploadDialogProps) => {
    const {
        acceptedExtensions,
        dragActive,
        fileInputRef,
        handleDragLeave,
        handleDragOver,
        handleDrop,
        handleFileInputChange,
        handleRemoveFile,
        handleUpload,
        isUploadPending,
        selectedFiles,
    } = useAiSkillUploadForm({onSuccess: () => onOpenChange(false)});

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent className="sm:max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Upload Skill Files</DialogTitle>
                </DialogHeader>

                <div
                    className={twMerge(
                        'flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-12 transition-colors',
                        dragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-gray-400'
                    )}
                    onClick={() => fileInputRef.current?.click()}
                    onDragLeave={handleDragLeave}
                    onDragOver={handleDragOver}
                    onDrop={handleDrop}
                >
                    <div className="flex flex-col items-center gap-2">
                        <UploadIcon className="size-10 text-gray-400" />

                        <p className="text-sm text-gray-600">
                            <span className="font-medium text-blue-600">Pick file(s)</span> to upload or drag and drop
                        </p>

                        <p className="text-xs text-gray-400">Accepted formats: {acceptedExtensions.join(', ')}</p>
                    </div>

                    <input
                        accept={acceptedExtensions.join(',')}
                        className="hidden"
                        multiple
                        onChange={handleFileInputChange}
                        ref={fileInputRef}
                        type="file"
                    />
                </div>

                {selectedFiles.length > 0 && (
                    <div className="space-y-2">
                        {selectedFiles.map((file, index) => (
                            <div
                                className="flex items-center gap-2 rounded border px-3 py-2"
                                key={`${file.name}-${index}`}
                            >
                                <FileIcon className="size-4 shrink-0 text-gray-400" />

                                <span className="flex-1 truncate text-sm">{file.name}</span>

                                <span className="shrink-0 text-xs text-content-neutral-secondary">
                                    {(file.size / 1024).toFixed(1)} KB
                                </span>

                                <Button
                                    className="shrink-0 text-gray-400 hover:text-gray-600 [&_svg]:size-4"
                                    icon={<XIcon />}
                                    onClick={() => handleRemoveFile(index)}
                                    size="iconXs"
                                    type="button"
                                    variant="ghost"
                                />
                            </div>
                        ))}
                    </div>
                )}

                <DialogFooter>
                    <Button onClick={() => onOpenChange(false)} variant="outline">
                        Cancel
                    </Button>

                    <Button disabled={selectedFiles.length === 0 || isUploadPending} onClick={handleUpload}>
                        {isUploadPending
                            ? 'Uploading...'
                            : `Upload${selectedFiles.length > 1 ? ` (${selectedFiles.length})` : ''}`}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiSkillUploadDialog;
