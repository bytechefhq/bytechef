import Dialog from './Dialog';
import React, { useState } from 'react';
import './styles.css';

// Define the return type explicitly to help TypeScript understand the structure
interface ConnectionDialogHook {
  openDialog: () => void;
  closeDialog: () => void;
  DialogComponent: React.FC;
}

export default function useEmbeddedByteChefConnectionDialog(): ConnectionDialogHook {
  const [isOpen, setIsOpen] = useState(false);
  const openDialog = () => {
    setIsOpen(true);
  };

  const closeDialog = () => {
    setIsOpen(false);
  };

  const handleConnect = () => {
    closeDialog();
  };

  // Define the component separately with explicit React.FC type
  const DialogComponent: React.FC = () => (
    <Dialog handleConnect={handleConnect} isOpen={isOpen} onClose={closeDialog} />
  );

  return {
    openDialog,
    closeDialog,
    DialogComponent,
  };
}
