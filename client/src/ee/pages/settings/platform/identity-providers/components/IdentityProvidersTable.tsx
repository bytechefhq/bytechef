import Button from '@/components/Button/Button';
import {Badge} from '@/components/ui/badge';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {IdentityProviderType} from '@/shared/middleware/graphql';
import {EditIcon, Trash2Icon} from 'lucide-react';

import useDeleteIdentityProviderAlertDialog from './hooks/useDeleteIdentityProviderAlertDialog';
import useIdentityProviderDialog from './hooks/useIdentityProviderDialog';
import useIdentityProvidersTable from './hooks/useIdentityProvidersTable';

const IdentityProvidersTable = () => {
    const {error, identityProviders, isLoading} = useIdentityProvidersTable();
    const {handleOpen: handleOpenDelete} = useDeleteIdentityProviderAlertDialog();
    const {handleOpenEdit} = useIdentityProviderDialog();

    if (error) {
        return <div className="text-destructive">Error: {(error as Error).message}</div>;
    }

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-12">
                <p className="text-muted-foreground">Loading identity providers...</p>
            </div>
        );
    }

    return (
        <Table>
            <TableHeader>
                <TableRow className="border-b-border/50">
                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Name
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Type
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Issuer / Metadata URI
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Domains
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Status
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                        Enforced
                    </TableHead>

                    <TableHead className="sticky top-0 z-10 bg-white p-3 text-left text-xs font-medium uppercase tracking-wide text-gray-500" />
                </TableRow>
            </TableHeader>

            <TableBody>
                {identityProviders.map((identityProvider: IdentityProviderType) => (
                    <TableRow className="cursor-pointer border-b-border/50" key={identityProvider.id}>
                        <TableCell className="whitespace-nowrap font-medium">{identityProvider.name}</TableCell>

                        <TableCell className="whitespace-nowrap">
                            <Badge variant="outline">{identityProvider.type}</Badge>
                        </TableCell>

                        <TableCell className="max-w-xs truncate whitespace-nowrap">
                            {identityProvider.type === 'SAML'
                                ? identityProvider.metadataUri
                                : identityProvider.issuerUri}
                        </TableCell>

                        <TableCell className="whitespace-nowrap">
                            <div className="flex flex-wrap gap-1">
                                {identityProvider.domains.map((domain) => (
                                    <Badge key={domain} variant="secondary">
                                        {domain}
                                    </Badge>
                                ))}
                            </div>
                        </TableCell>

                        <TableCell className="whitespace-nowrap">
                            <Badge variant={identityProvider.enabled ? 'default' : 'outline'}>
                                {identityProvider.enabled ? 'Enabled' : 'Disabled'}
                            </Badge>
                        </TableCell>

                        <TableCell className="whitespace-nowrap">{identityProvider.enforced ? 'Yes' : 'No'}</TableCell>

                        <TableCell className="flex justify-end whitespace-nowrap">
                            <Button
                                icon={<EditIcon className="size-4" />}
                                onClick={() => handleOpenEdit(identityProvider)}
                                size="icon"
                                variant="ghost"
                            />

                            <Button
                                icon={<Trash2Icon className="size-4 text-destructive" />}
                                onClick={() => handleOpenDelete(identityProvider.id)}
                                size="icon"
                                variant="ghost"
                            />
                        </TableCell>
                    </TableRow>
                ))}

                {identityProviders.length === 0 && (
                    <TableRow>
                        <TableCell className="px-4 py-6 text-center text-muted-foreground" colSpan={7}>
                            No identity providers configured.
                        </TableCell>
                    </TableRow>
                )}
            </TableBody>
        </Table>
    );
};

export default IdentityProvidersTable;
