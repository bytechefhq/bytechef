import {VariableFormValuesI, useVariablesProvider} from '@/ee/shared/components/variables/providers/variablesProvider';
import {useVariablesStore} from '@/ee/shared/components/variables/stores/useVariablesStore';
import {useShallow} from 'zustand/react/shallow';

const useVariables = () => {
    const {setCurrentVariable, setShowDeleteDialog, setShowEditDialog} = useVariablesStore(
        useShallow((state) => ({
            setCurrentVariable: state.setCurrentVariable,
            setShowDeleteDialog: state.setShowDeleteDialog,
            setShowEditDialog: state.setShowEditDialog,
        }))
    );

    const {
        canManage,
        useCreateVariableMutation,
        useDeleteVariableMutation,
        useUpdateVariableMutation,
        useVariablesQuery,
    } = useVariablesProvider();

    const {data: variables, error: variablesError, isLoading: variablesLoading} = useVariablesQuery();

    const createVariableMutation = useCreateVariableMutation({
        onSuccess: () => {
            setShowEditDialog(false);
            setCurrentVariable(undefined);
        },
    });

    const deleteVariableMutation = useDeleteVariableMutation({
        onSuccess: () => {
            setShowDeleteDialog(false);
            setCurrentVariable(undefined);
        },
    });

    const updateVariableMutation = useUpdateVariableMutation({
        onSuccess: () => {
            setShowEditDialog(false);
            setCurrentVariable(undefined);
        },
    });

    const handleDelete = (id: string) => deleteVariableMutation.mutate({id});

    const handleSave = (values: VariableFormValuesI, id?: string) =>
        id ? updateVariableMutation.mutate({id, ...values}) : createVariableMutation.mutate(values);

    return {canManage, handleDelete, handleSave, variables, variablesError, variablesLoading};
};

export default useVariables;
