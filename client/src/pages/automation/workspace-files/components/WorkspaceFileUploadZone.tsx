import {useWorkspaceFileUpload} from '@/pages/automation/workspace-files/hooks/useWorkspaceFileUpload';
import {DragEvent, ReactNode, useState} from 'react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

interface WorkspaceFileUploadZoneProps {
    children: ReactNode;
    onUploaded?: () => void;
    workspaceId: number;
}

interface UploadStateI {
    fileName: string;
    progress: number;
}

const WorkspaceFileUploadZone = ({children, onUploaded, workspaceId}: WorkspaceFileUploadZoneProps) => {
    const [isDragging, setIsDragging] = useState(false);
    const [activeUploads, setActiveUploads] = useState<UploadStateI[]>([]);

    const {upload} = useWorkspaceFileUpload();

    const handleDragOver = (event: DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        event.stopPropagation();

        setIsDragging(true);
    };

    const handleDragLeave = (event: DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        event.stopPropagation();

        setIsDragging(false);
    };

    const handleDrop = async (event: DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        event.stopPropagation();

        setIsDragging(false);

        const files = Array.from(event.dataTransfer.files);

        if (files.length === 0) {
            return;
        }

        for (const file of files) {
            setActiveUploads((previous) => [...previous, {fileName: file.name, progress: 0}]);

            try {
                await upload(workspaceId, file);

                setActiveUploads((previous) => previous.filter((upload) => upload.fileName !== file.name));

                onUploaded?.();
            } catch (error) {
                console.error('Upload failed', error);

                toast.error(`Upload failed: ${file.name}`);

                setActiveUploads((previous) => previous.filter((upload) => upload.fileName !== file.name));
            }
        }
    };

    return (
        <div
            className={twMerge(
                'relative flex size-full flex-col transition',
                isDragging && 'bg-primary/5 outline-dashed outline-2 outline-primary'
            )}
            data-testid="workspace-file-upload-zone"
            onDragLeave={handleDragLeave}
            onDragOver={handleDragOver}
            onDrop={handleDrop}
        >
            {children}

            {activeUploads.length > 0 && (
                <div className="pointer-events-none absolute bottom-4 right-4 z-10 flex flex-col gap-2">
                    {activeUploads.map((activeUpload) => (
                        <div
                            className="pointer-events-auto rounded-md border bg-background px-3 py-2 text-xs shadow-md"
                            key={activeUpload.fileName}
                        >
                            <span className="font-medium">Uploading {activeUpload.fileName}</span>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default WorkspaceFileUploadZone;
