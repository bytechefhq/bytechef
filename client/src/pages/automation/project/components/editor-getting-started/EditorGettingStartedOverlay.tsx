import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import useProjectHeaderDialogsStore from '@/pages/automation/project/stores/useProjectHeaderDialogsStore';
import {CSSProperties, useEffect, useRef, useState} from 'react';
import {createPortal} from 'react-dom';
import {twMerge} from 'tailwind-merge';

import EditorGettingStartedCard from './EditorGettingStartedCard';

interface EditorGettingStartedOverlayProps {
    projectId: number;
    projectWorkflowId: number;
}

const EditorGettingStartedOverlay = ({projectId, projectWorkflowId}: EditorGettingStartedOverlayProps) => {
    const [placement, setPlacement] = useState<CSSProperties | undefined>();

    const placeholderRef = useRef<HTMLDivElement>(null);

    const deploymentsSheetOpen = useProjectHeaderDialogsStore((state) => state.deploymentsSheetState !== undefined);
    const executionsSheetOpen = useProjectDeploymentWorkflowSheetStore(
        (state) => state.projectDeploymentWorkflowSheetOpen
    );

    const aboveSheets = deploymentsSheetOpen || executionsSheetOpen;

    useEffect(() => {
        const placeholder = placeholderRef.current;

        if (!placeholder) {
            return;
        }

        const updatePlacement = () => {
            const rect = placeholder.getBoundingClientRect();

            setPlacement({height: rect.height, left: rect.left, top: rect.top});
        };

        updatePlacement();

        const resizeObserver = new ResizeObserver(updatePlacement);

        resizeObserver.observe(placeholder);

        if (placeholder.parentElement) {
            resizeObserver.observe(placeholder.parentElement);
        }

        window.addEventListener('resize', updatePlacement);

        return () => {
            resizeObserver.disconnect();

            window.removeEventListener('resize', updatePlacement);
        };
    }, []);

    return (
        <>
            <div
                aria-hidden="true"
                className="pointer-events-none absolute top-16 bottom-20 left-6 w-0"
                data-testid="editor-getting-started-placeholder"
                ref={placeholderRef}
            />

            {placement &&
                createPortal(
                    <div
                        className={twMerge(
                            'pointer-events-none fixed flex flex-col justify-end',
                            aboveSheets ? 'z-[60]' : 'z-10'
                        )}
                        style={placement}
                    >
                        <div className="pointer-events-auto flex min-h-0 flex-col justify-end">
                            <EditorGettingStartedCard projectId={projectId} projectWorkflowId={projectWorkflowId} />
                        </div>
                    </div>,
                    document.body
                )}
        </>
    );
};

export default EditorGettingStartedOverlay;
