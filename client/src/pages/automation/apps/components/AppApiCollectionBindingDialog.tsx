import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {App, useAppBindableApiCollectionsQuery, useBindAppApiCollectionMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useMemo, useState} from 'react';

interface AppApiCollectionBindingDialogProps {
    app: App;
    boundApiCollectionIds?: Array<string>;
    triggerNode?: ReactNode;
}

const AppApiCollectionBindingDialog = ({
    app,
    boundApiCollectionIds = [],
    triggerNode,
}: AppApiCollectionBindingDialogProps) => {
    const [isOpen, setIsOpen] = useState(false);
    const [selectedApiCollectionId, setSelectedApiCollectionId] = useState('');

    const queryClient = useQueryClient();

    const {data: bindableApiCollectionsData} = useAppBindableApiCollectionsQuery({appId: app.id}, {enabled: isOpen});

    const bindAppApiCollectionMutation = useBindAppApiCollectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appApiCollections']});

            closeDialog();
        },
    });

    const apiCollectionItems = useMemo(
        () =>
            (bindableApiCollectionsData?.appBindableApiCollections ?? [])
                .filter((apiCollection) => !boundApiCollectionIds.includes(apiCollection.apiCollectionId))
                .map((apiCollection) => ({
                    label: `${apiCollection.name} — ${apiCollection.projectName || 'Unknown project'} (/${apiCollection.contextPath} v${apiCollection.collectionVersion}${apiCollection.available ? '' : ', disabled'})`,
                    value: apiCollection.apiCollectionId,
                })),
        [bindableApiCollectionsData?.appBindableApiCollections, boundApiCollectionIds]
    );

    function closeDialog() {
        setSelectedApiCollectionId('');

        setIsOpen(false);
    }

    function saveBinding() {
        bindAppApiCollectionMutation.mutate({apiCollectionId: selectedApiCollectionId, appId: app.id});
    }

    return (
        <Dialog
            onOpenChange={(nextOpen) => {
                if (nextOpen) {
                    setIsOpen(true);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent aria-describedby={undefined} size="md">
                <DialogMain>
                    <DialogHeader title="Bind API Collection" />

                    <DialogBody>
                        <fieldset className="space-y-2 border-0 p-0">
                            <p className="text-sm text-content-neutral-secondary">
                                The app serves the endpoints of the bound collection under its own API. A disabled
                                collection can be bound now and serves once its deployment is enabled.
                            </p>

                            <ComboBox
                                ariaLabel="API collection"
                                emptyMessage="No API collection is deployed in this app's workspace and environment."
                                items={apiCollectionItems}
                                onChange={(item) => setSelectedApiCollectionId(item?.value || '')}
                                value={selectedApiCollectionId}
                            />
                        </fieldset>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            disabled={!selectedApiCollectionId || bindAppApiCollectionMutation.isPending}
                            label="Bind"
                            onClick={saveBinding}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AppApiCollectionBindingDialog;
