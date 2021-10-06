package com.integri.atlas.workflow.taskhandler.s3;

import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import com.integri.atlas.workflow.core.task.TaskExecution;
import com.integri.atlas.workflow.core.task.TaskHandler;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Retrieves objects from Amazon S3. To use GET, Piper must have READ access to the object.
 *
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("s3/getObject")
class S3GetObject implements TaskHandler<Object> {

  @Override
  public Object handle (TaskExecution aTask) throws Exception {

    AmazonS3URI s3Uri = new AmazonS3URI(aTask.getRequiredString("uri"));

    String bucketName = s3Uri.getBucket();
    String key = s3Uri.getKey();

    S3Client s3 = S3Client.builder().build();

    s3.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build(),
        ResponseTransformer.toFile(Paths.get(aTask.getRequiredString("filepath"))));


    return null;
  }

}
