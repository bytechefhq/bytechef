import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipPortal, TooltipTrigger} from '@/components/ui/tooltip';

interface PropertyFormulaSwitchProps {
    formulaMode: boolean;
    handleClick: () => void;
}

const PropertyFormulaSwitch = ({formulaMode, handleClick}: PropertyFormulaSwitchProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <span className="inline-flex [&_[data-testid=switch-wrapper]>div]:gap-0.5">
                <Switch
                    checked={formulaMode}
                    label="Formula"
                    onCheckedChange={(checked) => {
                        if (checked !== formulaMode) {
                            handleClick();
                        }
                    }}
                    variant="small"
                />
            </span>
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>{formulaMode ? 'Switch to text' : 'Switch to formula'}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default PropertyFormulaSwitch;
