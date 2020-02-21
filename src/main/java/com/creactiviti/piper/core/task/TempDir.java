package com.creactiviti.piper.core.task;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
class TempDir implements MethodExecutor {

  @Override
  public TypedValue execute (EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    String tmpDir = System.getProperty("java.io.tmpdir");
    if(tmpDir.endsWith(File.separator)) {
      tmpDir = FilenameUtils.getFullPathNoEndSeparator(tmpDir);
    }
    return new TypedValue(tmpDir);
  }

}
