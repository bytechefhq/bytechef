import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {FormInputIcon, FunctionSquareIcon} from 'lucide-react';

const InputTypeSwitchButton = ({handleClick, mentionInput}: {handleClick: () => void; mentionInput: boolean}) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Button
                icon={mentionInput ? <FormInputIcon /> : <FunctionSquareIcon />}
                onClick={handleClick}
                size="iconXs"
                variant="secondary"
            />
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>{mentionInput ? 'Switch to constant value' : 'Switch to dynamic value'}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default InputTypeSwitchButton;
