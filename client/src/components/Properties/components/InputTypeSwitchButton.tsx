import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {FormInputIcon, FunctionSquareIcon} from 'lucide-react';

const InputTypeSwitchButton = ({
    handleInputTypeSwitchButtonClick,
    mentionInput,
}: {
    handleInputTypeSwitchButtonClick: () => void;
    mentionInput: boolean;
}) => {
    return (
        <Button
            className=" right-0 top-0 z-50 size-auto p-0.5"
            onClick={handleInputTypeSwitchButtonClick}
            size="icon"
            title="Switch input type"
            variant="ghost"
        >
            {mentionInput ? (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <FormInputIcon className="size-5 text-gray-800" />
                    </TooltipTrigger>

                    <TooltipContent>Switch to constant value</TooltipContent>
                </Tooltip>
            ) : (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <FunctionSquareIcon className="size-5 text-gray-800" />
                    </TooltipTrigger>

                    <TooltipContent>Switch to dynamic value</TooltipContent>
                </Tooltip>
            )}
        </Button>
    );
};

export default InputTypeSwitchButton;
