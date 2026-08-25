import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import useVariables from '@/ee/shared/components/variables/hooks/useVariables';
import {useVariablesStore} from '@/ee/shared/components/variables/stores/useVariablesStore';
import {useShallow} from 'zustand/react/shallow';

const VariableDeleteDialog = () => {
    const {currentVariable, setCurrentVariable, setShowDeleteDialog} = useVariablesStore(
        useShallow((state) => ({
            currentVariable: state.currentVariable,
            setCurrentVariable: state.setCurrentVariable,
            setShowDeleteDialog: state.setShowDeleteDialog,
        }))
    );

    const {handleDelete} = useVariables();

    const handleOpenChange = () => {
        setShowDeleteDialog(false);
        setCurrentVariable(undefined);
    };

    return (
        <AlertDialog onOpenChange={handleOpenChange} open={true}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Delete variable</AlertDialogTitle>

                    <AlertDialogDescription>
                        {`Are you sure you want to delete "${currentVariable?.name}"? Workflows referencing \${vars.${currentVariable?.name}} will see the literal expression instead of a value.`}
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>

                    {currentVariable && (
                        <AlertDialogAction className="bg-destructive" onClick={() => handleDelete(currentVariable.id)}>
                            Delete
                        </AlertDialogAction>
                    )}
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default VariableDeleteDialog;
