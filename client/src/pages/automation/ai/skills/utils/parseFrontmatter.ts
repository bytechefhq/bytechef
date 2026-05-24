export interface ParsedFrontmatterI {
    body: string;
    frontmatter: Record<string, string> | null;
    rawFrontmatter: string | null;
}

export default function parseFrontmatter(content: string): ParsedFrontmatterI {
    const frontmatterMatch = content.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/);

    if (!frontmatterMatch) {
        return {body: content, frontmatter: null, rawFrontmatter: null};
    }

    const frontmatterLines = frontmatterMatch[1].split('\n');
    const frontmatter: Record<string, string> = {};

    for (const line of frontmatterLines) {
        const colonIndex = line.indexOf(':');

        if (colonIndex > 0) {
            const key = line.substring(0, colonIndex).trim();
            const value = line.substring(colonIndex + 1).trim();

            frontmatter[key] = value.replace(/^"|"$/g, '');
        }
    }

    return {body: frontmatterMatch[2].trim(), frontmatter, rawFrontmatter: frontmatterMatch[1]};
}
