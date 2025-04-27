import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {FormInputIcon, FunctionSquareIcon} from 'lucide-react';

const InputTypeSwitchButton = ({handleClick, mentionInput}: {handleClick: () => void; mentionInput: boolean}) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Button className="size-auto p-0.5" onClick={handleClick} size="icon" variant="ghost">
                {mentionInput ? (
                    <FormInputIcon className="size-5 text-gray-600 hover:text-gray-800" />
                ) : (
                    <FunctionSquareIcon className="size-5 text-gray-600 hover:text-gray-800" />
                )}
            </Button>
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>{mentionInput ? 'Switch to constant value' : 'Switch to dynamic value'}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default InputTypeSwitchButton;
