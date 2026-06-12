import Button from '@/components/Button/Button';
import {InfoIcon, Undo2Icon, XIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const SubflowBanner = ({className}: {className?: string}) => {
    const [dismissed, setDismissed] = useState(false);

    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const {projectId, projectWorkflowId} = useParams();

    const fromSubflowParam = searchParams.get('fromSubflow');
    const parentProjectWorkflowIdParam = searchParams.get('parentProjectWorkflowId');

    const handleReturnClick = useCallback(() => {
        if (projectId && parentProjectWorkflowIdParam) {
            const newSearchParams = new URLSearchParams(searchParams.toString());

            const parentChain = newSearchParams.get('parentChain');

            if (parentChain) {
                const chainItems = parentChain.split(',');

                const grandparentId = chainItems.pop() ?? '';

                if (chainItems.length > 0) {
                    newSearchParams.set('parentChain', chainItems.join(','));
                } else {
                    newSearchParams.delete('parentChain');
                }

                newSearchParams.set('parentProjectWorkflowId', grandparentId);
            } else {
                newSearchParams.delete('fromSubflow');
                newSearchParams.delete('parentProjectWorkflowId');
                newSearchParams.set('restoreExecutionPanel', 'true');
            }

            navigate(
                `/automation/projects/${projectId}/project-workflows/${parentProjectWorkflowIdParam}?${newSearchParams}`
            );
        } else {
            navigate(-1);
        }
    }, [navigate, parentProjectWorkflowIdParam, projectId, searchParams]);

    const handleDismiss = useCallback(() => setDismissed(true), []);

    useEffect(() => {
        setDismissed(false);
    }, [projectWorkflowId]);

    if (fromSubflowParam !== 'true' || dismissed) {
        return null;
    }

    return (
        <div
            className={twMerge(
                'absolute top-2 left-2 z-10 flex w-[483px] items-center gap-2 rounded-md border border-stroke-warning-secondary bg-surface-warning-secondary px-3 py-2',
                className
            )}
        >
            <InfoIcon className="size-6 shrink-0 text-content-onwarning" />

            <span className="flex-1 text-sm font-medium whitespace-nowrap text-content-neutral-primary">
                Currently inside of a subflow.
            </span>

            <div className="flex shrink-0 items-center gap-1">
                <Button
                    className="active:text-content-primary text-sm font-medium hover:bg-transparent hover:underline active:bg-transparent"
                    icon={<Undo2Icon className="size-4" />}
                    label="Return to parent flow"
                    onClick={handleReturnClick}
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

export default SubflowBanner;
