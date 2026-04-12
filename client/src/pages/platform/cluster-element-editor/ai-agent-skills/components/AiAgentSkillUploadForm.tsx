import Button from '@/components/Button/Button';
import useAiAgentSkillUploadForm from '@/pages/platform/cluster-element-editor/ai-agent-skills/hooks/useAiAgentSkillUploadForm';
import {FileIcon, UploadIcon, XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

const AiAgentSkillUploadForm = () => {
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
    } = useAiAgentSkillUploadForm();

    return (
        <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col py-6">
            <fieldset className="border-0 p-0">
                <div
                    className={twMerge(
                        'mb-4 flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-12 transition-colors',
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
                    <div className="mb-4 space-y-2">
                        {selectedFiles.map((file, index) => (
                            <div
                                className="flex items-center gap-2 rounded border px-3 py-2"
                                key={`${file.name}-${index}`}
                            >
                                <FileIcon className="size-4 shrink-0 text-gray-400" />

                                <span className="flex-1 truncate text-sm">{file.name}</span>

                                <span className="shrink-0 text-xs text-gray-500">
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

                <Button disabled={selectedFiles.length === 0 || isUploadPending} onClick={handleUpload}>
                    {isUploadPending
                        ? 'Uploading...'
                        : `Upload${selectedFiles.length > 1 ? ` (${selectedFiles.length})` : ''}`}
                </Button>
            </fieldset>
        </div>
    );
};

export default AiAgentSkillUploadForm;
