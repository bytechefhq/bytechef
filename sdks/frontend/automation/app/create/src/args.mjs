import {parseArgs} from 'node:util';

const PACKAGE_MANAGERS = ['npm', 'pnpm', 'yarn', 'bun'];
const NPM_HINT =
    'Under npm, flags must follow "--": npm create bytechef-app@latest my-app -- --url https://… --no-install';
const LINE_BREAK = /[\r\n]/;

export const USAGE = `Usage:
  npm create bytechef-app@latest [directory] -- [flags]
  pnpm create bytechef-app [directory] [flags]
  npx create-bytechef-app [directory] [flags]

Under npm create, flags must follow "--"; npm takes any flag before it as its own option.

Arguments and flags:
  [directory]                    Directory to create (default bytechef-app); its name becomes the package name,
                                 and @scope/name creates name/ with the package name @scope/name
  --url <url>                    ByteChef base URL (http or https) written to .env.local (falls back to BYTECHEF_URL)
  --no-install                   Skip installing dependencies
  --package-manager <manager>    One of ${PACKAGE_MANAGERS.join(', ')}; detected from the invoking package manager
  -h, --help                     Show this help

There is no flag for the app key: a key on the command line ends up in shell history and process listings.
Set BYTECHEF_APP_KEY in the environment before scaffolding, or in .env.local afterwards.`;

export function statusCommand(packageManager) {
    return packageManager === 'npm' ? 'npm run -s bytechef:status' : `${packageManager} run bytechef:status`;
}

function validateUrl(url) {
    if (LINE_BREAK.test(url)) {
        throw new Error('--url must not contain a line break');
    }

    let parsed;

    try {
        parsed = new URL(url);
    } catch {
        throw new Error(`--url must be an http or https URL, got: ${url}`);
    }

    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
        throw new Error(`--url must be an http or https URL, got: ${url}`);
    }
}

export function parseCliArgs(argv, env = process.env) {
    let parsed;

    try {
        parsed = parseArgs({
            allowNegative: true,
            allowPositionals: true,
            args: argv,
            options: {
                help: {short: 'h', type: 'boolean'},
                install: {type: 'boolean'},
                'package-manager': {type: 'string'},
                url: {type: 'string'},
            },
            strict: true,
        });
    } catch (error) {
        throw new Error(`${error.message}. ${NPM_HINT}`);
    }

    if (parsed.values.help) {
        return {help: true};
    }

    if (env.npm_config_url !== undefined && env.npm_config_url !== '' && parsed.values.url === undefined) {
        throw new Error(`npm took --url as its own option. ${NPM_HINT}`);
    }

    if (env.npm_config_install !== undefined && parsed.values.install === undefined) {
        throw new Error(`npm took --no-install as its own option. ${NPM_HINT}`);
    }

    if (parsed.positionals.length > 1) {
        throw new Error(`Only one directory may be given, got: ${parsed.positionals.join(' ')}. ${NPM_HINT}`);
    }

    const packageManager = parsed.values['package-manager'];

    if (packageManager !== undefined && !PACKAGE_MANAGERS.includes(packageManager)) {
        throw new Error(`--package-manager must be one of ${PACKAGE_MANAGERS.join(', ')}`);
    }

    if (parsed.values.url !== undefined) {
        validateUrl(parsed.values.url);
    }

    return {
        directory: parsed.positionals[0] ?? 'bytechef-app',
        help: false,
        install: parsed.values.install ?? true,
        packageManager,
        url: parsed.values.url,
    };
}
