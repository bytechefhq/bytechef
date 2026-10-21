#!/usr/bin/env node
import {readFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

import {parseCliArgs, statusCommand, USAGE} from '../src/args.mjs';
import {buildEnvLocal, detectPackageManager, nodeVersionWarning, resolveTarget} from '../src/env.mjs';
import {installCommand, runInstall} from '../src/install.mjs';
import {lockfileRemovedMessage, scaffold} from '../src/scaffold.mjs';

const packageDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const {version} = JSON.parse(readFileSync(resolve(packageDir, 'package.json'), 'utf8'));

function main() {
    let options;

    try {
        options = parseCliArgs(process.argv.slice(2));
    } catch (error) {
        console.error(error.message);
        process.exit(2);
    }

    if (options.help) {
        console.log(USAGE);

        return;
    }

    const nodeWarning = nodeVersionWarning();

    if (nodeWarning) {
        console.warn(nodeWarning);
    }

    const target = resolveTarget(options.directory);
    const packageManager = options.packageManager ?? detectPackageManager();
    const envLocal = buildEnvLocal({url: options.url});

    const {lockfileRemoved} = scaffold({
        appVersion: version,
        envLocal: envLocal.content,
        packageManager,
        packageName: target.packageName,
        targetDir: target.targetDir,
        templateDir: resolve(packageDir, 'template'),
    });

    console.log(`Created ${target.targetDir}`);

    if (lockfileRemoved) {
        console.log(lockfileRemovedMessage(packageManager));
    }

    if (envLocal.keyFromEnvironment) {
        console.log('BYTECHEF_APP_KEY taken from the environment and written to .env.local');
    }

    if (options.install) {
        const {message, status} = runInstall(packageManager, target.targetDir);

        if (status !== 0) {
            const {args, command} = installCommand(packageManager);

            console.error(
                `Installing dependencies failed${message ? ` (${message})` : ''}; run "${command} ${args.join(' ')}" in ${target.targetDir}`
            );
            process.exit(status);
        }
    }

    const installStep = options.install
        ? ''
        : `Run ${packageManager} install before deploying: the lockfile gains @bytechef/app on the first install.\n  `;

    console.log(`
Next steps:
  cd ${target.directory}
  ${installStep}${envLocal.keyFromEnvironment ? '' : 'Set BYTECHEF_APP_KEY in .env.local (from the app page in ByteChef)\n  '}${packageManager} run dev
  ${statusCommand(packageManager)}
  ${packageManager} run check
  Deploy: Vercel or Docker, see README.md
`);
}

try {
    main();
} catch (error) {
    console.error(error.message);
    process.exit(1);
}
