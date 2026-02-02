import {useState} from 'react';

export default function useKnowledgeBaseDropdownMenu() {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    return {
        handleCloseDeleteDialog: () => setShowDeleteDialog(false),
        handleCloseEditDialog: () => setShowEditDialog(false),
        handleShowDeleteDialog: () => setShowDeleteDialog(true),
        handleShowEditDialog: () => setShowEditDialog(true),
        showDeleteDialog,
        showEditDialog,
    };
}
