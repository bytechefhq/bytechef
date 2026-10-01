import Button from '@/components/Button/Button';
import {Separator} from '@/components/ui/separator';
import {DownloadIcon, EditIcon, HistoryIcon, Trash2Icon} from 'lucide-react';
import {MouseEvent} from 'react';

interface AgentTabButtonsProps {
    isDeletePending: boolean;
    onCloseDropdownMenu: () => void;
    onDeleteClick: () => void;
    onExportClick: () => void;
    onShowEditDialog: () => void;
    onShowVersionHistorySheet: () => void;
}

const AgentTabButtons = ({
    isDeletePending,
    onCloseDropdownMenu,
    onDeleteClick,
    onExportClick,
    onShowEditDialog,
    onShowVersionHistorySheet,
}: AgentTabButtonsProps) => {
    const handleButtonClick = (event: MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLElement).tagName === 'BUTTON') {
            onCloseDropdownMenu();
        }
    };

    return (
        <div className="flex flex-col" onClick={handleButtonClick}>
            <Button
                className="dropdown-menu-item"
                icon={<EditIcon />}
                label="Edit"
                onClick={onShowEditDialog}
                variant="ghost"
            />

            <Button
                className="dropdown-menu-item"
                icon={<HistoryIcon />}
                label="Agent History"
                onClick={onShowVersionHistorySheet}
                variant="ghost"
            />

            <Button
                className="dropdown-menu-item"
                icon={<DownloadIcon />}
                label="Export"
                onClick={onExportClick}
                variant="ghost"
            />

            <Separator />

            <Button
                className="dropdown-menu-item-destructive"
                disabled={isDeletePending}
                icon={<Trash2Icon />}
                label="Delete"
                onClick={onDeleteClick}
                variant="ghost"
            />
        </div>
    );
};

export default AgentTabButtons;
