import {DataPillType} from '@/shared/types';

const SEGMENT_SEPARATORS = ['.', '['];

enum MatchTierType {
    PREFIX,
    SEGMENT_START,
    SUBSTRING,
}

interface DataPillMatchI {
    start: number;
    tier: MatchTierType;
}

// The best occurrence of `query` in `value`: the path prefix, else the first occurrence that begins a
// segment (right after `.` or `[`), else the first occurrence anywhere. Both arguments are lowercase.
function findBestMatch(value: string, query: string): DataPillMatchI | null {
    const firstStart = value.indexOf(query);

    if (firstStart === -1) {
        return null;
    }

    if (firstStart === 0) {
        return {start: 0, tier: MatchTierType.PREFIX};
    }

    for (let start = firstStart; start !== -1; start = value.indexOf(query, start + 1)) {
        if (SEGMENT_SEPARATORS.includes(value[start - 1])) {
            return {start, tier: MatchTierType.SEGMENT_START};
        }
    }

    return {start: firstStart, tier: MatchTierType.SUBSTRING};
}

export function filterDataPillSuggestions(dataPills: DataPillType[], query: string): DataPillType[] {
    if (!query) {
        return dataPills;
    }

    const lowercaseQuery = query.toLowerCase();

    // Array.prototype.sort is stable, so each tier keeps the panel's parent-before-child order.
    return dataPills
        .map((dataPill) => ({dataPill, match: findBestMatch(dataPill.value.toLowerCase(), lowercaseQuery)}))
        .filter((candidate) => candidate.match !== null)
        .sort((first, second) => first.match!.tier - second.match!.tier)
        .map((candidate) => candidate.dataPill);
}

export function findDataPillMatchRange(value: string, query: string): {end: number; start: number} | null {
    if (!query) {
        return null;
    }

    const match = findBestMatch(value.toLowerCase(), query.toLowerCase());

    return match ? {end: match.start + query.length, start: match.start} : null;
}
