import {spawnSync} from 'node:child_process';

export function installCommand(packageManager, platform = process.platform) {
    return {args: ['install'], command: packageManager, shell: platform === 'win32'};
}

export function runInstall(packageManager, cwd) {
    const {args, command, shell} = installCommand(packageManager);
    const result = shell
        ? spawnSync(`${command} ${args.join(' ')}`, {cwd, shell, stdio: 'inherit'})
        : spawnSync(command, args, {cwd, stdio: 'inherit'});

    return {message: result.error?.message, status: result.status ?? 1};
}
