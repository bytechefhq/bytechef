import {useState} from 'react';

export default function useKnowledgeBaseDropdownMenu() {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const handleShowDeleteDialog = () => {
        setShowDeleteDialog(true);
    };

    const handleCloseDeleteDialog = () => {
        setShowDeleteDialog(false);
    };

    const handleShowEditDialog = () => {
        setShowEditDialog(true);
    };

    const handleCloseEditDialog = () => {
        setShowEditDialog(false);
    };

    return {
        handleCloseDeleteDialog,
        handleCloseEditDialog,
        handleShowDeleteDialog,
        handleShowEditDialog,
        showDeleteDialog,
        showEditDialog,
    };
}
