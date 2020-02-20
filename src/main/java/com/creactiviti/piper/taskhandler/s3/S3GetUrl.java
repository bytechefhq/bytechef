package com.creactiviti.piper.taskhandler.s3;

import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

/**
 * Returns the URL for an object stored in Amazon S3. If the object identified 
 * by the given bucket and key has public read permissions, then this URL can 
 * be directly accessed to retrieve the object's data.
 * 
 * @author Arik Cohen
 * @since Feb, 20 2020
 */
@Component("s3/get-url")
class S3GetUrl implements TaskHandler<String> {

  @Override
  public String handle (Task aTask) throws Exception {
    
    AmazonS3URI s3Uri = new AmazonS3URI(aTask.getRequiredString("uri"));
    
    String bucketName = s3Uri.getBucket();
    String key = s3Uri.getKey();
    
    S3Client s3 = S3Client.builder().build();
    
    return s3.utilities().getUrl(
      GetUrlRequest.builder()
                   .bucket(bucketName)
                   .key(key)
                   .build()
    ).toString();
    
  }

}
