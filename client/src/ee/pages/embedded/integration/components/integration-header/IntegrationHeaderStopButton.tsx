import {Button} from '@/components/ui/button';
import {SquareIcon} from 'lucide-react';

const IntegrationHeaderStopButton = ({onClick}: {onClick?: () => void}) => (
    <Button className="hover:bg-background/70 [&_svg]:size-5" onClick={onClick} size="icon" variant="ghost">
        <SquareIcon className="text-destructive" />
    </Button>
);

export default IntegrationHeaderStopButton;
