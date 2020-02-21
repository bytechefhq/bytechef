package com.creactiviti.piper.taskhandler.s3;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Presign a GetObjectRequest so that it can be executed at a later time without 
 * requiring additional signing or authentication.
 * 
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("s3/presignGetObject")
class S3PresignedGetObject implements TaskHandler<Object> {

  @Override
  public Object handle (TaskExecution aTask) throws Exception {
    AmazonS3URI s3Uri = new AmazonS3URI(aTask.getRequiredString("uri"));
    
    S3Presigner presigner = S3Presigner.create();
    
    PresignedGetObjectRequest presignedRequest =
        presigner.presignGetObject(z -> z.signatureDuration(Duration.parse("PT"+aTask.getRequiredString("signatureDuration")))
                .getObjectRequest(por -> por.bucket(s3Uri.getBucket()).key(s3Uri.getKey())));
    
    
    return presignedRequest.url().toString();
  }

}
