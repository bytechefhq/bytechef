import {filterAllowedFiles} from '@/ee/pages/automation/ai-hub/composer/hooks/useAiHubAttachmentUpload';
import {DragEvent, ReactNode, useCallback, useRef, useState} from 'react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

interface AiHubComposerDropZonePropsI {
    children: ReactNode;
    className?: string;
    // Drag-drop uploads are suppressed when true (e.g. workflow chats, which forward messages to a
    // webhook and cannot consume attached resources). The wrapper div is still rendered so the surrounding
    // composer layout is unchanged — only the drag handlers and the "Drop files to attach" overlay are off.
    disabled?: boolean;
    onFilesDropped: (files: File[]) => void;
}

const AiHubComposerDropZone = ({
    children,
    className,
    disabled = false,
    onFilesDropped,
}: AiHubComposerDropZonePropsI) => {
    const dragCounterRef = useRef(0);

    const [isDragActive, setIsDragActive] = useState(false);

    const handleDragEnter = useCallback(
        (event: DragEvent<HTMLDivElement>) => {
            event.preventDefault();
            event.stopPropagation();

            if (disabled) {
                return;
            }

            dragCounterRef.current += 1;

            if (event.dataTransfer.types.includes('Files')) {
                setIsDragActive(true);
            }
        },
        [disabled]
    );

    const handleDragOver = useCallback(
        (event: DragEvent<HTMLDivElement>) => {
            event.preventDefault();
            event.stopPropagation();

            if (disabled) {
                return;
            }

            if (event.dataTransfer.types.includes('Files')) {
                event.dataTransfer.dropEffect = 'copy';
            }
        },
        [disabled]
    );

    const handleDragLeave = useCallback((event: DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        event.stopPropagation();

        dragCounterRef.current -= 1;

        if (dragCounterRef.current <= 0) {
            dragCounterRef.current = 0;

            setIsDragActive(false);
        }
    }, []);

    const handleDrop = useCallback(
        (event: DragEvent<HTMLDivElement>) => {
            event.preventDefault();
            event.stopPropagation();

            dragCounterRef.current = 0;

            setIsDragActive(false);

            if (disabled) {
                return;
            }

            const dropped = Array.from(event.dataTransfer.files ?? []);

            if (dropped.length === 0) {
                return;
            }

            const {allowed, rejected} = filterAllowedFiles(dropped);

            if (rejected.length > 0) {
                const rejectedNames = rejected.map((file) => file.name).join(', ');

                toast.error(`File type not supported for: ${rejectedNames}`);
            }

            if (allowed.length > 0) {
                onFilesDropped(allowed);
            }
        },
        [disabled, onFilesDropped]
    );

    return (
        <div
            className={twMerge('relative', className)}
            data-testid="ai-hub-composer-drop-zone"
            onDragEnter={handleDragEnter}
            onDragLeave={handleDragLeave}
            onDragOver={handleDragOver}
            onDrop={handleDrop}
        >
            {children}

            {isDragActive && (
                <div
                    className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center rounded-md border-2 border-dashed border-primary bg-primary/10 text-sm font-medium text-primary"
                    data-testid="ai-hub-composer-drop-overlay"
                >
                    Drop files to attach
                </div>
            )}
        </div>
    );
};

export default AiHubComposerDropZone;
