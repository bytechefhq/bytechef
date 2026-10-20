import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {ApiCollection, ApiCollectionVersionUpdate, ResponseError} from '@/ee/shared/middleware/automation/api-platform';
import {useUpdateApiCollectionProjectVersionMutation} from '@/ee/shared/mutations/automation/apiCollections.mutations';
import {ApiCollectionKeys} from '@/ee/shared/mutations/automation/apiCollections.queries';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {useGetProjectVersionsQuery} from '@/shared/queries/automation/projectVersions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

const API_COLLECTION_ENTITY_CLASS = 'ApiCollection';
const ENDPOINT_WORKFLOW_REMOVED_ERROR_KEY = 105;

interface ApiCollectionErrorBodyI {
    entityClass?: string;
    errorKey?: number;
    inputParameters?: {endpointNames?: string[]};
}

interface ApiCollectionProjectVersionDialogProps {
    apiCollection: ApiCollection;
    onClose: () => void;
}

const readRemovedEndpointNames = async (error: Error): Promise<string[] | undefined> => {
    if (!(error instanceof ResponseError)) {
        return undefined;
    }

    try {
        const errorBody = (await error.response.clone().json()) as ApiCollectionErrorBodyI;

        if (
            errorBody.entityClass !== API_COLLECTION_ENTITY_CLASS ||
            errorBody.errorKey !== ENDPOINT_WORKFLOW_REMOVED_ERROR_KEY
        ) {
            return undefined;
        }

        return errorBody.inputParameters?.endpointNames || [];
    } catch {
        return undefined;
    }
};

const ApiCollectionProjectVersionDialog = ({apiCollection, onClose}: ApiCollectionProjectVersionDialogProps) => {
    const [removedEndpointNames, setRemovedEndpointNames] = useState<string[]>();
    const [selectedProjectVersion, setSelectedProjectVersion] = useState<number>();
    const [versionUpdate, setVersionUpdate] = useState<ApiCollectionVersionUpdate>();

    const {data: projectVersions, isPending: projectVersionsPending} = useGetProjectVersionsQuery(
        apiCollection.projectId
    );

    const queryClient = useQueryClient();

    const updateApiCollectionProjectVersionMutation = useUpdateApiCollectionProjectVersionMutation({
        onError: (error) => {
            readRemovedEndpointNames(error).then((endpointNames) => {
                if (endpointNames) {
                    setRemovedEndpointNames(endpointNames);
                }
            });
        },
        onSuccess: (result) => {
            queryClient.invalidateQueries({queryKey: ApiCollectionKeys.apiCollections});

            setRemovedEndpointNames(undefined);
            setVersionUpdate(result);
        },
    });

    const newerPublishedProjectVersions = useMemo(
        () =>
            (projectVersions || []).filter(
                (projectVersion) =>
                    projectVersion.status === ProjectStatus.Published &&
                    projectVersion.version !== undefined &&
                    projectVersion.version > apiCollection.projectVersion
            ),
        [apiCollection.projectVersion, projectVersions]
    );

    const updateProjectVersion = (removeMissingEndpoints: boolean) => {
        if (selectedProjectVersion === undefined) {
            return;
        }

        updateApiCollectionProjectVersionMutation.mutate({
            id: apiCollection.id!,
            projectVersion: selectedProjectVersion,
            removeMissingEndpoints,
        });
    };

    const handleProjectVersionChange = (value: string) => {
        setRemovedEndpointNames(undefined);
        setSelectedProjectVersion(+value);
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Move the collection to a newer published project version. Its endpoint URLs stay the same."
                        title="Update Project Version"
                    />

                    <DialogBody>
                        {versionUpdate ? (
                            <div className="space-y-3 text-sm">
                                <p>
                                    {`${apiCollection.name} now uses project version V${versionUpdate.apiCollection?.projectVersion}.`}
                                </p>

                                {!!versionUpdate.removedEndpointNames?.length && (
                                    <p>{`Removed endpoints: ${versionUpdate.removedEndpointNames.join(', ')}`}</p>
                                )}

                                {!!versionUpdate.disabledEndpointNames?.length && (
                                    <p className="text-destructive">
                                        {`Disabled endpoints: ${versionUpdate.disabledEndpointNames.join(', ')}`}
                                    </p>
                                )}

                                {!!versionUpdate.missingConnections?.length && (
                                    <div className="space-y-1">
                                        <p className="font-medium">These endpoints need a connection:</p>

                                        <ul className="list-disc pl-5">
                                            {versionUpdate.missingConnections.map((missingConnection) => (
                                                <li
                                                    key={`${missingConnection.endpointName}-${missingConnection.workflowNodeName}-${missingConnection.key}`}
                                                >
                                                    {`${missingConnection.endpointName}: ${missingConnection.componentName} (${missingConnection.workflowNodeName})`}
                                                </li>
                                            ))}
                                        </ul>
                                    </div>
                                )}

                                {!!versionUpdate.missingInputs?.length && (
                                    <div className="space-y-1">
                                        <p className="font-medium">These endpoints need an input value:</p>

                                        <ul className="list-disc pl-5">
                                            {versionUpdate.missingInputs.map((missingInput) => (
                                                <li key={`${missingInput.endpointName}-${missingInput.inputName}`}>
                                                    {`${missingInput.endpointName}: ${missingInput.inputName}`}
                                                </li>
                                            ))}
                                        </ul>
                                    </div>
                                )}
                            </div>
                        ) : (
                            <fieldset className="space-y-4 border-0">
                                {!projectVersionsPending && newerPublishedProjectVersions.length === 0 ? (
                                    <p className="text-sm text-muted-foreground">
                                        {`There is no published project version newer than V${apiCollection.projectVersion}.`}
                                    </p>
                                ) : (
                                    <RadioGroup
                                        onValueChange={handleProjectVersionChange}
                                        value={selectedProjectVersion?.toString() || ''}
                                    >
                                        {newerPublishedProjectVersions.map((projectVersion) => (
                                            <div className="flex items-center gap-2" key={projectVersion.version}>
                                                <RadioGroupItem
                                                    id={`api-collection-project-version-${projectVersion.version}`}
                                                    value={projectVersion.version!.toString()}
                                                />

                                                <Label
                                                    htmlFor={`api-collection-project-version-${projectVersion.version}`}
                                                >
                                                    V{projectVersion.version}
                                                </Label>

                                                {projectVersion.description && (
                                                    <span className="text-xs text-muted-foreground">
                                                        {projectVersion.description}
                                                    </span>
                                                )}
                                            </div>
                                        ))}
                                    </RadioGroup>
                                )}

                                {removedEndpointNames && (
                                    <div className="space-y-2 rounded-md border border-destructive p-3 text-sm">
                                        <p>
                                            Version V{selectedProjectVersion} no longer has the workflows of these
                                            endpoints:
                                        </p>

                                        <ul className="list-disc pl-5">
                                            {removedEndpointNames.map((endpointName) => (
                                                <li key={endpointName}>{endpointName}</li>
                                            ))}
                                        </ul>

                                        <Button
                                            disabled={updateApiCollectionProjectVersionMutation.isPending}
                                            label="Remove these endpoints"
                                            onClick={() => updateProjectVersion(true)}
                                            type="button"
                                            variant="destructive"
                                        />
                                    </div>
                                )}
                            </fieldset>
                        )}
                    </DialogBody>

                    <DialogFooter>
                        {versionUpdate ? (
                            <Button label="Close" onClick={onClose} type="button" />
                        ) : (
                            <>
                                <DialogCancelButton />

                                <Button
                                    disabled={
                                        selectedProjectVersion === undefined ||
                                        updateApiCollectionProjectVersionMutation.isPending
                                    }
                                    label="Update"
                                    onClick={() => updateProjectVersion(false)}
                                    type="button"
                                />
                            </>
                        )}
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ApiCollectionProjectVersionDialog;
