import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import VariableDeleteDialog from '@/ee/shared/components/variables/components/VariableDeleteDialog';
import VariableDialog from '@/ee/shared/components/variables/components/VariableDialog';
import VariableTable from '@/ee/shared/components/variables/components/VariableTable';
import useVariables from '@/ee/shared/components/variables/hooks/useVariables';
import {useVariablesStore} from '@/ee/shared/components/variables/stores/useVariablesStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {VariableIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

interface VariablesContentProps {
    description: string;
    title: string;
}

const VariablesContent = ({description, title}: VariablesContentProps) => {
    const {setShowEditDialog, showDeleteDialog, showEditDialog} = useVariablesStore(
        useShallow((state) => ({
            setShowEditDialog: state.setShowEditDialog,
            showDeleteDialog: state.showDeleteDialog,
            showEditDialog: state.showEditDialog,
        }))
    );

    const {canManage, variables, variablesError, variablesLoading} = useVariables();

    return (
        <PageLoader errors={[variablesError]} loading={variablesLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description={description}
                        position="main"
                        right={
                            canManage && variables && variables.length > 0 ? (
                                <div className="flex items-center gap-4">
                                    <EnvironmentSelect />

                                    <VariableDialog triggerNode={<Button>New Variable</Button>} />
                                </div>
                            ) : (
                                <EnvironmentSelect />
                            )
                        }
                        title={title}
                    />
                }
                leftSidebarOpen={false}
            >
                {variables && variables.length > 0 ? (
                    <VariableTable variables={variables} />
                ) : (
                    <EmptyList
                        button={
                            canManage ? (
                                <Button onClick={() => setShowEditDialog(true)}>New Variable</Button>
                            ) : undefined
                        }
                        icon={<VariableIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by defining a variable, then reference it in any workflow as ${vars.NAME}."
                        title="No Variables"
                    />
                )}

                {showDeleteDialog && <VariableDeleteDialog />}

                {showEditDialog && <VariableDialog />}
            </LayoutContainer>
        </PageLoader>
    );
};

export default VariablesContent;
