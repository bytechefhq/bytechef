/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.ollama.constant;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public final class OllamaConstants {

    public static final String URL = "url";
    public static final String FORMAT = "format";
    public static final String F16KV = "f16kv";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String LOGTS_ALL = "logitsAll";
    public static final String LOW_VRAM = "lowVram";
    public static final String MAIN_GPU = "mainGpu";
    public static final String MIROSTAT = "mirostat";
    public static final String MIROSTAT_ETA = "mirostatEta";
    public static final String MIROSTAT_TAU = "mirostatTau";
    public static final String NUM_BATCH = "numBatch";
    public static final String NUM_CTX = "numCtx";
    public static final String NUM_KEEP = "numKeep";
    public static final String NUM_GPU = "numGpu";
    public static final String NUM_THREAD = "numThread";
    public static final String PENALIZE_NEW_LINE = "penalizeNewLine";
    public static final String REPEAT_LAST_N = "repeatLastN";
    public static final String REPEAT_PENALTY = "repeatPenalty";
    public static final String TFSZ = "tfsz";
    public static final String TRUNCATE = "truncate";
    public static final String TYPICAL_P = "typicalP";
    public static final String USE_MLOCK = "useMlock";
    public static final String USE_MMAP = "useMmap";
    public static final String USE_NUMA = "useNuma";
    public static final String VOCAB_ONLY = "vocabOnly";

    private OllamaConstants() {
    }
}
