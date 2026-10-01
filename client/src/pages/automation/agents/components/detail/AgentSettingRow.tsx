import {Label} from '@/components/ui/label';
import {type ReactNode} from 'react';

interface AgentSettingRowProps {
    control: ReactNode;
    controlId?: string;
    description?: ReactNode;
    label: ReactNode;
}

const AgentSettingRow = ({control, controlId, description, label}: AgentSettingRowProps) => (
    <div className="flex items-center justify-between gap-6">
        <div className="flex flex-col gap-0.5">
            <Label className="text-sm leading-5 font-medium text-content-neutral-primary" htmlFor={controlId}>
                {label}
            </Label>

            {description && (
                <span className="text-sm leading-5 font-normal text-content-neutral-secondary">{description}</span>
            )}
        </div>

        <div className="shrink-0">{control}</div>
    </div>
);

export default AgentSettingRow;
