/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.script.task.handler;

//import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;

import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.workflow.worker.task.handler.AbstractTaskHandler;
//import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
// RUBY-DISABLED: the @Component registration is commented out because org.graalvm.polyglot:ruby is published
// only up to 25.0.0 and crashes on the pinned Truffle 25.2.4; the "script/v1/ruby" action it handles is no
// longer registered by ScriptComponentHandler. Re-enable together with those registrations once a polyglot
// ruby jar built on Truffle 25.2+ is published (or GraalVM is downgraded). Grep RUBY-DISABLED.
//@Component(SCRIPT + "/v1/ruby")
public class ScriptRubyTaskHandler extends AbstractTaskHandler {

    public ScriptRubyTaskHandler(ActionDefinitionFacade actionDefinitionFacade) {
        super("script", 1, "ruby", actionDefinitionFacade);
    }
}
