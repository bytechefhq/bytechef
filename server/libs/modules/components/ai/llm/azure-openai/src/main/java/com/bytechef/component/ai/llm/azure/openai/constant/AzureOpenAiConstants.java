package com.bytechef.component.ai.llm.azure.openai.constant;

import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.Option;
import org.springframework.ai.azure.openai.AzureOpenAiImageOptions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AzureOpenAiConstants {

    public static final List<Option<String>> MODELS = LLMUtils.getEnumOptions(
        Arrays.stream(AzureOpenAiImageOptions.ImageModel.values())
            .collect(
                Collectors.toMap(
                    AzureOpenAiImageOptions.ImageModel::getValue,
                    AzureOpenAiImageOptions.ImageModel::getValue)));

    private AzureOpenAiConstants() {}
}
