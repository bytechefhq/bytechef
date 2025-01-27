import {Button} from '@/components/ui/button';
import {SquareIcon} from 'lucide-react';

const ProjectHeaderStopButton = ({onStopClick}: {onStopClick: () => void}) => (
    <Button className="w-20 shadow-none" onClick={onStopClick} variant="destructive">
        <SquareIcon /> Stop
    </Button>
);

export default ProjectHeaderStopButton;
