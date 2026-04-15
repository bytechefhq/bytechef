import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {AuditEventsQuery} from '@/shared/middleware/graphql';
import {VisuallyHidden} from 'radix-ui';

type AuditEventItemType = NonNullable<AuditEventsQuery['auditEvents']['content']>[number];

interface AuditEventDetailSheetPropsI {
    auditEvent?: AuditEventItemType;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const AuditEventDetailSheet = ({auditEvent, onOpenChange, open}: AuditEventDetailSheetPropsI) => {
    return (
        <Sheet onOpenChange={onOpenChange} open={open}>
            <VisuallyHidden.Root>
                <SheetTitle>Audit Event Details</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="bottom-4 right-4 top-3 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[480px]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <div className="flex flex-col">
                        <span className="text-lg font-semibold">{auditEvent?.eventType || 'Audit Event'}</span>

                        <span className="text-xs text-muted-foreground">
                            {auditEvent?.eventDate ? new Date(auditEvent.eventDate).toLocaleString() : ''}
                        </span>
                    </div>

                    <SheetCloseButton />
                </header>

                <div className="flex min-h-0 flex-1 flex-col gap-6 overflow-y-auto p-4">
                    {auditEvent && (
                        <>
                            <dl className="grid grid-cols-[120px_1fr] gap-x-4 gap-y-2 text-sm">
                                <dt className="text-muted-foreground">Principal</dt>

                                <dd>{auditEvent.principal || 'anonymous'}</dd>

                                <dt className="text-muted-foreground">Event ID</dt>

                                <dd className="font-mono">{auditEvent.id}</dd>
                            </dl>

                            {auditEvent.data.length > 0 && (
                                <div className="flex flex-col gap-2">
                                    <span className="text-sm font-semibold">Data</span>

                                    <dl className="grid grid-cols-[120px_1fr] gap-x-4 gap-y-2 rounded-md border bg-surface-neutral-primary p-3 text-sm">
                                        {auditEvent.data.map((entry) => (
                                            <div className="contents" key={entry.key}>
                                                <dt className="break-words text-muted-foreground">{entry.key}</dt>

                                                <dd className="break-all font-mono">{entry.value}</dd>
                                            </div>
                                        ))}
                                    </dl>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default AuditEventDetailSheet;
