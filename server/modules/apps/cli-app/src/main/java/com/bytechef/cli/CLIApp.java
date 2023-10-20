/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli;

import static picocli.CommandLine.Command;

import com.bytechef.cli.cmd.component.ComponentCommand;
import picocli.CommandLine;

/**
 * @author Ivica Cardic
 */
@Command(
        name = "bytechef",
        description = "Executes various bytechef related commands.",
        mixinStandardHelpOptions = true,
        version = "bytechef 1.0",
        subcommands = {ComponentCommand.class})
public class CLIApp implements Runnable {

    public CLIApp() {}

    @Override
    public void run() {}

    public static void main(String... args) {
        int exitCode = new CommandLine(new CLIApp()).execute(args);

        System.exit(exitCode);
    }
}
