import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {useToast} from '@/hooks/use-toast';
import {useSessionsStore} from '@/pages/account/settings/stores/useSessionsStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {ShellIcon} from 'lucide-react';
import {useEffect} from 'react';
import {useShallow} from 'zustand/react/shallow';

const Sessions = () => {
    const account = useAuthenticationStore((state) => state.account);
    const {getSessions, invalidateSession, loading, reset, sessions, updateFailure, updateSuccess} = useSessionsStore(
        useShallow((state) => ({
            getSessions: state.getSessions,
            invalidateSession: state.invalidateSession,
            loading: state.loading,
            reset: state.reset,
            sessions: state.sessions,
            updateFailure: state.updateFailure,
            updateSuccess: state.updateSuccess,
        }))
    );

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
                    right={<Button label="Refresh" onClick={refreshList} />}
                    title={`Active sessions for [${account?.login}]`}
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader loading={loading}>
                {sessions && sessions?.length > 0 ? (
                    <div className="w-full px-2 3xl:mx-auto 3xl:w-4/5">
                        <Table className="table-auto">
                            <TableHeader>
                                <TableRow className="border-b-border/50">
                                    <TableHead>IP Address</TableHead>

                                    <TableHead>User agent</TableHead>

                                    <TableHead>Date</TableHead>

                                    <TableHead />
                                </TableRow>
                            </TableHeader>

                            <TableBody>
                                {sessions.map((s, index) => (
                                    <TableRow className="cursor-pointer border-b-border/50" key={index}>
                                        <TableCell>{s.ipAddress}</TableCell>

                                        <TableCell>
                                            <div className="max-w-lg truncate">{s.userAgent}</div>
                                        </TableCell>

                                        <TableCell>{s.tokenDate}</TableCell>

                                        <TableCell className="flex justify-end">
                                            <Button label="Invalidate" onClick={doSessionInvalidation(s.series)} />
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </div>
                ) : (
                    <EmptyList icon={<ShellIcon className="size-24 text-gray-300" />} title="No active sessions" />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Sessions;
