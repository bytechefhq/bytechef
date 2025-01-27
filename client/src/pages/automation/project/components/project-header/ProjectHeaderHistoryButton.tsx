import {Button} from '@/components/ui/button';
import {HistoryIcon} from 'lucide-react';

interface ProjectHeaderHistoryButtonProps {
    onClick: () => void;
}

const ProjectHeaderHistoryButton = ({onClick}: ProjectHeaderHistoryButtonProps) => {
    return (
        <Button className="justify-start hover:bg-surface-neutral-primary-hover" onClick={onClick} variant="ghost">
            <HistoryIcon /> Project History
        </Button>
    );
};

export default ProjectHeaderHistoryButton;
