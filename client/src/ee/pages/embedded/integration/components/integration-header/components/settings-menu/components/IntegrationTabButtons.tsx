import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {Separator} from '@/components/ui/separator';
import {EditIcon, HistoryIcon, Trash2Icon, UploadIcon} from 'lucide-react';
import {ChangeEvent, MouseEvent, useRef} from 'react';

const IntegrationTabButtons = ({
    onCloseDropdownMenuClick,
    onDeleteIntegrationClick,
    onImportWorkflow,
    onShowEditIntegrationDialogClick,
    onShowIntegrationVersionHistorySheet,
}: {
    onCloseDropdownMenuClick: () => void;
    onDeleteIntegrationClick: () => void;
    onImportWorkflow: (workflowDefinition: string) => void;
    onShowEditIntegrationDialogClick: () => void;
    onShowIntegrationVersionHistorySheet: () => void;
}) => {
    const hiddenFileInputRef = useRef<HTMLInputElement>(null);

    const handleButtonClick = (event: MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLElement).tagName === 'BUTTON') {
            onCloseDropdownMenuClick();
        }
    };

    const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            onImportWorkflow(await event.target.files[0].text());

            if (hiddenFileInputRef.current) {
                hiddenFileInputRef.current.value = '';
            }
        }
    };

    return (
        <>
            <div className="flex flex-col" onClick={handleButtonClick}>
                <Button
                    aria-label="Edit Integration Button"
                    className="dropdown-menu-item"
                    icon={<EditIcon />}
                    label="Edit"
                    onClick={() => onShowEditIntegrationDialogClick()}
                    variant="ghost"
                />

                <Button
                    aria-label="Import Workflow Button"
                    className="dropdown-menu-item"
                    icon={<UploadIcon />}
                    label="Import Workflow"
                    onClick={() => {
                        if (hiddenFileInputRef.current) {
                            hiddenFileInputRef.current.click();
                        }
                    }}
                    variant="ghost"
                />

                <Separator />

                <Button
                    aria-label="Integration History"
                    className="dropdown-menu-item"
                    icon={<HistoryIcon />}
                    label="Integration History"
                    onClick={onShowIntegrationVersionHistorySheet}
                    variant="ghost"
                />

                <Separator />

                <Button
                    aria-label="Delete Integration"
                    className="dropdown-menu-item-destructive"
                    icon={<Trash2Icon />}
                    label="Delete"
                    onClick={onDeleteIntegrationClick}
                    variant="ghost"
                />
            </div>

            <input
                accept=".json,.yaml,.yml"
                className="hidden"
                onChange={handleFileChange}
                ref={hiddenFileInputRef}
                type="file"
            />
        </>
    );
};

export default IntegrationTabButtons;
