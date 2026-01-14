import {Skeleton} from '@/components/ui/skeleton';
import {TableCell, TableRow} from '@/components/ui/table';

export default function UsersTableSkeleton() {
    return (
        <>
            {[1, 2, 3].map((index) => (
                <TableRow className="border-b-border/50" key={index}>
                    <TableCell>
                        <Skeleton className="h-4 w-48" />
                    </TableCell>

                    <TableCell>
                        <Skeleton className="h-4 w-32" />
                    </TableCell>

                    <TableCell>
                        <Skeleton className="h-4 w-24" />
                    </TableCell>

                    <TableCell>
                        <Skeleton className="h-4 w-16" />
                    </TableCell>

                    <TableCell className="flex justify-end gap-1">
                        <Skeleton className="size-8 rounded" />

                        <Skeleton className="size-8 rounded" />
                    </TableCell>
                </TableRow>
            ))}
        </>
    );
}
