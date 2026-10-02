import {type DragEvent, useCallback, useEffect, useState} from 'react';

import {CanvasDragKindType, getCanvasDragKind} from '../utils/canvasDragData';

export default function useCanvasDropzone(acceptedDragKind: CanvasDragKindType) {
    const [isDropzoneActive, setDropzoneActive] = useState(false);

    const isAcceptedDrag = useCallback(
        (event: DragEvent) => getCanvasDragKind(event.dataTransfer) === acceptedDragKind,
        [acceptedDragKind]
    );

    const handleDragEnter = useCallback(
        (event: DragEvent) => {
            if (isAcceptedDrag(event)) {
                setDropzoneActive(true);
            }
        },
        [isAcceptedDrag]
    );

    const handleDragLeave = useCallback((event: DragEvent) => {
        const relatedTarget = event.relatedTarget as Node | null;

        if (!relatedTarget || !event.currentTarget.contains(relatedTarget)) {
            setDropzoneActive(false);
        }
    }, []);

    const handleDragOver = useCallback(
        (event: DragEvent) => {
            if (!isAcceptedDrag(event)) {
                return;
            }

            event.preventDefault();

            setDropzoneActive(true);
        },
        [isAcceptedDrag]
    );

    const handleDrop = useCallback(() => setDropzoneActive(false), []);

    useEffect(() => {
        const handleGlobalDragEnd = () => setDropzoneActive(false);

        document.addEventListener('dragend', handleGlobalDragEnd);
        document.addEventListener('drop', handleGlobalDragEnd);

        return () => {
            document.removeEventListener('dragend', handleGlobalDragEnd);
            document.removeEventListener('drop', handleGlobalDragEnd);
        };
    }, []);

    return {
        dropzoneHandlers: {
            onDragEnter: handleDragEnter,
            onDragLeave: handleDragLeave,
            onDragOver: handleDragOver,
            onDrop: handleDrop,
        },
        isDropzoneActive,
    };
}
