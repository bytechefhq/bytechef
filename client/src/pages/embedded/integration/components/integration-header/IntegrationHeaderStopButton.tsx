import {Button} from '@/components/ui/button';
import {SquareIcon} from 'lucide-react';

const IntegrationHeaderStopButton = () => (
    <Button
        className="hover:bg-background/70"
        onClick={() => {
            // TODO
        }}
        size="icon"
        variant="ghost"
    >
        <SquareIcon className="h-5 text-destructive" />
    </Button>
);

export default IntegrationHeaderStopButton;
