import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipPortal, TooltipTrigger} from '@/components/ui/tooltip';

interface PropertyFormulaSwitchProps {
    disabled?: boolean;
    formulaMode: boolean;
    handleClick?: () => void;
}

const PropertyFormulaSwitch = ({disabled = false, formulaMode, handleClick}: PropertyFormulaSwitchProps) => {
    let tooltip = formulaMode ? 'Switch to text' : 'Switch to formula';

    if (disabled) {
        tooltip = 'This field always takes a formula';
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className="inline-flex [&_[data-testid=switch-wrapper]>div]:gap-1">
                    <Switch
                        checked={formulaMode}
                        disabled={disabled}
                        label="Formula"
                        onCheckedChange={(checked) => {
                            if (!disabled && checked !== formulaMode) {
                                handleClick?.();
                            }
                        }}
                        variant="small"
                    />
                </span>
            </TooltipTrigger>

            <TooltipPortal>
                <TooltipContent>{tooltip}</TooltipContent>
            </TooltipPortal>
        </Tooltip>
    );
};

export default PropertyFormulaSwitch;
