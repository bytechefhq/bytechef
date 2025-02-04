import {Button} from '@/components/ui/button';
import {HistoryIcon} from 'lucide-react';

interface ProjectHeaderHistoryButtonProps {
    handleShowProjectVersionHistorySheet: () => void;
}

const ProjectHeaderHistoryButton = ({handleShowProjectVersionHistorySheet}: ProjectHeaderHistoryButtonProps) => {
    return (
        <Button
            className="justify-start rounded-none hover:bg-surface-neutral-primary-hover"
            onClick={handleShowProjectVersionHistorySheet}
            variant="ghost"
        >
            <HistoryIcon /> Project History
        </Button>
    );
};

export default ProjectHeaderHistoryButton;
