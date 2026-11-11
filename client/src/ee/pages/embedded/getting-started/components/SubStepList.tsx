import {EmbeddedGettingStartedSubStepI} from '@/ee/pages/embedded/getting-started/getEmbeddedGettingStartedSubSteps';
import {CheckIcon, CircleIcon} from 'lucide-react';
import {Link} from 'react-router-dom';

const PENDING_LABEL_CLASS_NAME = 'text-left font-medium text-content-brand-primary hover:underline';

interface SubStepListProps {
    ariaLabel: string;
    subStepActions?: Partial<Record<string, () => void>>;
    subSteps: EmbeddedGettingStartedSubStepI[];
}

const SubStepList = ({ariaLabel, subStepActions, subSteps}: SubStepListProps) => (
    <ul aria-label={ariaLabel} className="mt-2 space-y-2">
        {subSteps.map((subStep) => {
            const subStepAction = subStepActions?.[subStep.id];

            return (
                <li className="flex items-start gap-2 text-sm" data-done={subStep.done} key={subStep.id}>
                    {subStep.done ? (
                        <CheckIcon aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-content-success-primary" />
                    ) : (
                        <CircleIcon
                            aria-hidden="true"
                            className="mt-0.5 size-4 shrink-0 text-content-neutral-tertiary"
                        />
                    )}

                    <div className="flex flex-col items-start">
                        {subStep.done && <span className="text-content-neutral-secondary">{subStep.label}</span>}

                        {!subStep.done && subStepAction && (
                            <button className={PENDING_LABEL_CLASS_NAME} onClick={subStepAction} type="button">
                                {subStep.label}
                            </button>
                        )}

                        {!subStep.done && !subStepAction && (
                            <Link className={PENDING_LABEL_CLASS_NAME} to={subStep.href}>
                                {subStep.label}
                            </Link>
                        )}

                        <span className="text-xs text-content-neutral-secondary">{subStep.hint}</span>
                    </div>
                </li>
            );
        })}
    </ul>
);

export default SubStepList;
