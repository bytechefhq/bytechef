import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipPortal, TooltipTrigger} from '@/components/ui/tooltip';

interface PropertyDynamicSwitchProps {
    dynamic: boolean;
    handleClick: () => void;
}

/**
 * Swaps a control that has nothing to type into (a select, an object or array builder) for the data pill editor and
 * back; the keyboard way in is `$`.
 */
const PropertyDynamicSwitch = ({dynamic, handleClick}: PropertyDynamicSwitchProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <span className="mr-3 inline-flex">
                <Switch
                    checked={dynamic}
                    label="Dynamic"
                    onCheckedChange={(checked) => {
                        if (checked !== dynamic) {
                            handleClick();
                        }
                    }}
                    variant="small"
                />
            </span>
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>{dynamic ? 'Switch to a constant value' : 'Switch to a data pill'}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default PropertyDynamicSwitch;
