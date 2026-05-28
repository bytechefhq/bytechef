import {InfoIcon, Undo2Icon, XIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const SubflowBanner = ({className}: {className?: string}) => {
    const [dismissed, setDismissed] = useState(false);

    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const {projectId, projectWorkflowId} = useParams();

    const fromSubflow = searchParams.get('fromSubflow');
    const parentProjectWorkflowId = searchParams.get('parentProjectWorkflowId');

    useEffect(() => {
        setDismissed(false);
    }, [projectWorkflowId]);

    if (fromSubflow !== 'true' || dismissed) {
        return null;
    }

    const handleReturnClick = () => {
        if (projectId && parentProjectWorkflowId) {
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
            }

            navigate(
                `/automation/projects/${projectId}/project-workflows/${parentProjectWorkflowId}?${newSearchParams}`
            );
        } else {
            navigate(-1);
        }
    };

    return (
        <div
            className={twMerge(
                'absolute left-2 top-2 z-10 flex w-[483px] items-center gap-2 rounded-md border border-stroke-warning-secondary bg-surface-warning-secondary px-3 py-2',
                className
            )}
        >
            <InfoIcon className="size-6 shrink-0 text-content-onwarning" />

            <span className="flex-1 text-sm font-medium text-content-neutral-primary">
                Currently inside of a subflow.
            </span>

            <button
                className="flex items-center gap-1 text-sm font-medium text-content-neutral-primary hover:underline"
                onClick={handleReturnClick}
            >
                <Undo2Icon className="size-4" />

                <span>Return to parent flow</span>
            </button>

            <button className="opacity-50 hover:opacity-100" onClick={() => setDismissed(true)}>
                <XIcon className="size-4 text-content-neutral-primary" />
            </button>
        </div>
    );
};

export default SubflowBanner;
