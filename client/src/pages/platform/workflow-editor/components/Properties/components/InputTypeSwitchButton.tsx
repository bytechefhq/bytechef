import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {FormInputIcon, FunctionSquareIcon} from 'lucide-react';

const InputTypeSwitchButton = ({handleClick, mentionInput}: {handleClick: () => void; mentionInput: boolean}) => (
    <Button className="size-auto p-0.5" onClick={handleClick} size="icon" title="Switch input type" variant="ghost">
        {mentionInput ? (
            <Tooltip>
                <TooltipTrigger asChild>
                    <FormInputIcon className="size-5 text-gray-600 hover:text-gray-800" />
                </TooltipTrigger>

                <TooltipPortal>
                    <TooltipContent>Switch to constant value</TooltipContent>
                </TooltipPortal>
            </Tooltip>
        ) : (
            <Tooltip>
                <TooltipTrigger asChild>
                    <FunctionSquareIcon className="size-5 text-gray-600 hover:text-gray-800" />
                </TooltipTrigger>

                <TooltipPortal>
                    <TooltipContent>Switch to dynamic value</TooltipContent>
                </TooltipPortal>
            </Tooltip>
        )}
    </Button>
);

export default InputTypeSwitchButton;
