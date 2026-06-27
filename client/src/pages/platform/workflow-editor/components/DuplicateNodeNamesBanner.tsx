import Button from '@/components/Button/Button';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {AlertTriangleIcon, FileJsonIcon, XIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import getDuplicateNodeNames from '../utils/getDuplicateNodeNames';

const DuplicateNodeNamesBanner = ({className}: {className?: string}) => {
    const [dismissed, setDismissed] = useState(false);

    const workflow = useWorkflowDataStore((state) => state.workflow);
    const setShowWorkflowCodeEditorSheet = useWorkflowEditorStore((state) => state.setShowWorkflowCodeEditorSheet);

    const duplicateNodeNames = useMemo(
        () => getDuplicateNodeNames(workflow.tasks, workflow.triggers),
        [workflow.tasks, workflow.triggers]
    );

    const handleOpenCodeEditorClick = useCallback(
        () => setShowWorkflowCodeEditorSheet(true),
        [setShowWorkflowCodeEditorSheet]
    );

    const handleDismiss = useCallback(() => setDismissed(true), []);

    useEffect(() => {
        setDismissed(false);
    }, [duplicateNodeNames]);

    if (duplicateNodeNames.length === 0 || dismissed) {
        return null;
    }

    return (
        <div
            className={twMerge(
                'bg-surface-error-secondary absolute top-2 left-2 z-10 flex w-[520px] items-center gap-2 rounded-md border border-stroke-destructive-secondary px-3 py-2',
                className
            )}
        >
            <AlertTriangleIcon className="size-6 shrink-0 text-content-destructive" />

            <span className="flex-1 text-sm font-medium text-content-neutral-primary">
                {duplicateNodeNames.length === 1 ? 'Duplicate node name: ' : 'Duplicate node names: '}

                <span className="font-semibold">{duplicateNodeNames.join(', ')}</span>

                {
                    '. Node names must be unique — the graph may render incorrectly until this is fixed in the code editor.'
                }
            </span>

            <div className="flex shrink-0 items-center gap-1">
                <Button
                    className="active:text-content-primary text-sm font-medium hover:bg-transparent hover:underline active:bg-transparent"
                    icon={<FileJsonIcon className="size-4" />}
                    label="Open code editor"
                    onClick={handleOpenCodeEditorClick}
                    size="xs"
                    variant="ghost"
                />

                <Button
                    className="active:text-content-primary opacity-50 hover:bg-transparent hover:opacity-100 active:bg-transparent"
                    icon={<XIcon />}
                    onClick={handleDismiss}
                    size="iconXs"
                    variant="ghost"
                />
            </div>
        </div>
    );
};

export default DuplicateNodeNamesBanner;
