import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {useToast} from '@/components/ui/use-toast';
import {useSessionsStore} from '@/pages/account/settings/stores/useSessionsStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {ShellIcon} from 'lucide-react';
import React, {useEffect} from 'react';

const Sessions = () => {
    const {account} = useAuthenticationStore();
    const {getSessions, invalidateSession, loading, reset, sessions, updateFailure, updateSuccess} = useSessionsStore();

    const {toast} = useToast();

    const doSessionInvalidation = (series: string) => () => {
        invalidateSession(series);
    };

    const refreshList = () => {
        getSessions();
    };

    useEffect(() => {
        getSessions();

        return () => {
            reset();
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (updateSuccess) {
            toast({description: 'The session has been invalidated.'});
        }

        if (updateFailure) {
            toast({description: 'The session could not be invalidated.'});
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [updateSuccess]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={<Button onClick={refreshList}>Refresh</Button>}
                    title={`Active sessions for [${account?.login}]`}
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader loading={loading}>
                {sessions && sessions?.length > 0 ? (
                    <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                        <Table className="table-auto">
                            <TableHeader>
                                <TableRow>
                                    <TableHead>IP Address</TableHead>

                                    <TableHead>User agent</TableHead>

                                    <TableHead>Date</TableHead>

                                    <TableHead />
                                </TableRow>
                            </TableHeader>

                            <TableBody>
                                {sessions.map((s, index) => (
                                    <TableRow key={index}>
                                        <TableCell>{s.ipAddress}</TableCell>

                                        <TableCell>
                                            <div className="max-w-lg truncate">{s.userAgent}</div>
                                        </TableCell>

                                        <TableCell>{s.tokenDate}</TableCell>

                                        <TableCell className="flex justify-end">
                                            <Button onClick={doSessionInvalidation(s.series)}>Invalidate</Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </div>
                ) : (
                    <EmptyList icon={<ShellIcon className="size-12 text-gray-400" />} title="No active sessions" />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Sessions;
