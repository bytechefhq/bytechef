import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {TooltipPortal} from '@radix-ui/react-tooltip';

const PropertyInputTypeSwitch = ({handleClick, mentionInput}: {handleClick: () => void; mentionInput: boolean}) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <span className="inline-flex">
                <Switch
                    checked={mentionInput}
                    label="Dynamic"
                    onCheckedChange={(checked) => {
                        if (checked !== mentionInput) {
                            handleClick();
                        }
                    }}
                    variant="small"
                />
            </span>
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>{mentionInput ? 'Switch to constant value' : 'Switch to dynamic value'}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default PropertyInputTypeSwitch;
