import {Button} from '@/components/ui/button';
import {SquareIcon} from 'lucide-react';

const ProjectHeaderStopButton = () => (
    <Button
        className="hover:bg-background/70 [&_svg]:size-5"
        onClick={() => {
            // TODO
        }}
        size="icon"
        variant="ghost"
    >
        <SquareIcon className="text-destructive" />
    </Button>
);

export default ProjectHeaderStopButton;
