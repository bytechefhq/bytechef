import {tableFeatures} from '@tanstack/react-table';

/**
 * The feature set shared by every table in the client.
 *
 * TanStack Table v9 requires each table to declare the features it uses so unused ones can be
 * tree-shaken. Every table here renders a static list, so the core features that `useTable` always
 * installs (columns, rows, headers, cells, core row model) are sufficient and no extra feature is
 * registered. A table that needs sorting, filtering, pagination or column visibility should declare
 * its own feature set rather than widening this one.
 */
export const coreTableFeatures = tableFeatures({});
