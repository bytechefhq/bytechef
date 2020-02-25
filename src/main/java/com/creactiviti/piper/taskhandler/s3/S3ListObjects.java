package com.creactiviti.piper.taskhandler.s3;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;
import com.creactiviti.piper.taskhandler.s3.S3ListObjects.S3ObjectDescription;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * @author Arik Cohen
 * @since Feb, 24 2020
 */
@Component("s3/listObjects")
class S3ListObjects implements TaskHandler<List<S3ObjectDescription>> {

  @Override
  public List<S3ObjectDescription> handle (TaskExecution aTask) throws Exception {
    
    S3Client s3 = S3Client.builder().build();
    
    ListObjectsResponse response = s3.listObjects(ListObjectsRequest.builder()
                                     .bucket(aTask.getRequiredString("bucket"))
                                     .prefix(aTask.getRequiredString("prefix"))
                                     .build());
    
    return response.contents()
                   .stream()
                   .map(o->new S3ObjectDescription(aTask.getRequiredString("bucket"),o))
                   .collect(Collectors.toList());
  }
  
  static final class S3ObjectDescription {
    
    private final String bucket;
    private final S3Object s3Object;
    
    public S3ObjectDescription(String aBucket, S3Object aS3Object) {
      bucket = aBucket;
      s3Object = aS3Object;
    }
    
    public String getKey () {
      return s3Object.key();
    }
    
    public String getSuffix () {
      return FilenameUtils.getName(getKey());
    }
    
    public String getBucket () {
      return bucket;
    }
    
    public String getUri () {
      return String.format("s3://%s/%s", getBucket(), getKey());
    }
    
  }

}
