import Button from '@/components/Button/Button';
import {SquareIcon} from 'lucide-react';

const IntegrationHeaderStopButton = ({onClick}: {onClick?: () => void}) => (
    <Button
        className="hover:bg-background/70 [&_svg]:size-5"
        icon={<SquareIcon className="text-destructive" />}
        onClick={onClick}
        size="icon"
        variant="ghost"
    />
);

export default IntegrationHeaderStopButton;
