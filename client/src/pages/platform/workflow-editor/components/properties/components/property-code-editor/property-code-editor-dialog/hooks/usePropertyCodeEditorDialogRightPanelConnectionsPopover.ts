import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {zodResolver} from '@hookform/resolvers/zod';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

import {connectionFormSchema} from '../PropertyCodeEditorDialogRightPanelConnectionsPopover';

const usePropertyCodeEditorDialogRightPanelConnectionsPopover = () => {
    const [open, setOpen] = useState(false);

    const form = useForm<z.infer<typeof connectionFormSchema>>({
        defaultValues: {
            componentName: '',
            name: '',
        },
        resolver: zodResolver(connectionFormSchema),
    });

    const {useGetComponentDefinitionsQuery} = useWorkflowEditor();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery!({connectionDefinitions: true});

    return {
        componentDefinitions,
        form,
        open,
        setOpen,
    };
};

export default usePropertyCodeEditorDialogRightPanelConnectionsPopover;
