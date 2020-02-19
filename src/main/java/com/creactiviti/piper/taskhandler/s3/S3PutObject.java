package com.creactiviti.piper.taskhandler.s3;

import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Adds an object to a bucket. Piper must have WRITE permissions on a bucket to add an object to it.
 * 
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("s3/put-object")
class S3PutObject implements TaskHandler<Object> {

  @Override
  public Object handle (Task aTask) throws Exception {
    
    AmazonS3URI s3Uri = new AmazonS3URI(aTask.getRequiredString("uri"));
    
    String bucketName = s3Uri.getBucket();
    String key = s3Uri.getKey();
    
    S3Client s3 = S3Client.builder().build();
    
    s3.putObject(PutObjectRequest.builder()
                                 .bucket(bucketName)
                                 .key(key)
                                 .build(), Paths.get(aTask.getRequiredString("filepath")));
    
    return null;
  }

}
