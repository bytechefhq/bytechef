import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ColumnType, useCreateDataTableMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

type ColumnDefinitionType = {name: string; type: ColumnType};

interface UseCreateDataTableDialogI {
    baseName: string;
    canSubmit: boolean;
    columns: ColumnDefinitionType[];
    description: string;
    handleAddColumn: () => void;
    handleBaseNameChange: (value: string) => void;
    handleClose: () => void;
    handleColumnNameChange: (index: number, name: string) => void;
    handleColumnTypeChange: (index: number, type: ColumnType) => void;
    handleCreate: () => void;
    handleDescriptionChange: (value: string) => void;
    handleOpen: () => void;
    handleOpenChange: (open: boolean) => void;
    handleRemoveColumn: (index: number) => void;
    isPending: boolean;
    open: boolean;
}

export default function useCreateDataTableDialog(): UseCreateDataTableDialogI {
    const [open, setOpen] = useState(false);
    const [baseName, setBaseName] = useState('');
    const [columns, setColumns] = useState<ColumnDefinitionType[]>([{name: '', type: ColumnType.String}]);
    const [description, setDescription] = useState('');

    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const createMutation = useCreateDataTableMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            setOpen(false);
            setBaseName('');
            setColumns([{name: '', type: ColumnType.String}]);
            setDescription('');
        },
    });

    const canSubmit = baseName.trim().length > 0 && columns.every((column) => column.name.trim().length > 0);

    const handleOpen = () => {
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            handleClose();
        }
    };

    const handleAddColumn = () => {
        setColumns((previous) => [...previous, {name: '', type: ColumnType.String}]);
    };

    const handleRemoveColumn = (index: number) => {
        setColumns((previous) => previous.filter((_, columnIndex) => columnIndex !== index));
    };

    const handleColumnNameChange = (index: number, name: string) => {
        setColumns((previous) => {
            const previousColumns = [...previous];
            previousColumns[index] = {...previousColumns[index], name};

            return previousColumns;
        });
    };

    const handleColumnTypeChange = (index: number, type: ColumnType) => {
        setColumns((previous) => {
            const previousColumns = [...previous];
            previousColumns[index] = {...previousColumns[index], type};

            return previousColumns;
        });
    };

    const handleBaseNameChange = (value: string) => {
        setBaseName(value);
    };

    const handleDescriptionChange = (value: string) => {
        setDescription(value);
    };

    const handleCreate = () => {
        createMutation.mutate({
            input: {
                baseName: baseName.trim(),
                columns: columns.map((column) => ({name: column.name.trim(), type: column.type})),
                description: description.trim() || undefined,
                environmentId: String(environmentId),
                workspaceId: String(workspaceId),
            },
        });
    };

    return {
        baseName,
        canSubmit,
        columns,
        description,
        handleAddColumn,
        handleBaseNameChange,
        handleClose,
        handleColumnNameChange,
        handleColumnTypeChange,
        handleCreate,
        handleDescriptionChange,
        handleOpen,
        handleOpenChange,
        handleRemoveColumn,
        isPending: createMutation.isPending,
        open,
    };
}
