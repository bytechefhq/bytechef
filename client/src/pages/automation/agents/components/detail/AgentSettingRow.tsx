import {Label} from '@/components/ui/label';
import {type ReactNode} from 'react';

interface AgentSettingRowProps {
    control: ReactNode;
    /** Ties the label to its control, so clicking the text operates the switch, select or input on the right. */
    controlId?: string;
    description?: ReactNode;
    label: ReactNode;
}

/**
 * One row of the Settings tab: what the setting is on the left, the control that changes it on the right.
 *
 * Switches, selects and inputs all render through this, so every control lines up in a single right-hand column
 * instead of each one sitting wherever its own label happens to end — which is what a bare `Switch` with a label
 * does, since its wrapper is `w-fit`.
 */
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
