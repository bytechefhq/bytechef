import {PillTargetI} from '@/pages/platform/workflow-editor/components/datapills/pillTarget';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {DataPillDragPayloadType} from '@/shared/types';
import {DragEvent, useCallback, useEffect, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface UsePillTargetPropsI {
    acceptsPill: () => boolean;
    insertPill: (mentionId: string) => void;
}

const DATA_PILL_MIME_TYPE = 'application/bytechef-datapill';

export default function usePillTarget({acceptsPill, insertPill}: UsePillTargetPropsI) {
    const acceptsPillRef = useRef(acceptsPill);
    const insertPillRef = useRef(insertPill);
    const ownerRef = useRef<HTMLDivElement | null>(null);

    acceptsPillRef.current = acceptsPill;
    insertPillRef.current = insertPill;

    const {clearPillTarget, setPillTarget} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            clearPillTarget: state.clearPillTarget,
            setPillTarget: state.setPillTarget,
        }))
    );

    const onFocusCapture = useCallback(() => {
        const pillTarget: PillTargetI = {
            acceptsPill: () => acceptsPillRef.current(),
            insertPill: (mentionId) => insertPillRef.current(mentionId),
            owner: ownerRef.current,
        };

        setPillTarget(pillTarget);
    }, [setPillTarget]);

    const onDragOver = useCallback((event: DragEvent<HTMLDivElement>) => {
        if (event.dataTransfer.types.includes(DATA_PILL_MIME_TYPE) && acceptsPillRef.current()) {
            event.preventDefault();

            event.dataTransfer.dropEffect = 'copy';
        }
    }, []);

    const onDrop = useCallback((event: DragEvent<HTMLDivElement>) => {
        const rawPayload = event.dataTransfer.getData(DATA_PILL_MIME_TYPE);

        if (!rawPayload || !acceptsPillRef.current()) {
            return;
        }

        event.preventDefault();

        try {
            const payload = JSON.parse(rawPayload) as DataPillDragPayloadType;

            if (payload?.mentionId) {
                insertPillRef.current(payload.mentionId);
            }
        } catch {
            return;
        }
    }, []);

    useEffect(() => {
        const owner = ownerRef.current;

        return () => clearPillTarget(owner);
    }, [clearPillTarget]);

    return {onDragOver, onDrop, onFocusCapture, ref: ownerRef};
}
