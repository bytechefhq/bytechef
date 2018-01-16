
package com.creactiviti.piper.core.event;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.creactiviti.piper.core.Accessor;
import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.MapObject;
import com.creactiviti.piper.core.job.Job;
import com.creactiviti.piper.core.job.JobRepository;

/**
 * 
 * @author Arik Cohen
 * @since Jun 9, 2017
 */
public class JobStatusWebhookEventListener implements EventListener {
  
  private final JobRepository jobRepository;
  
  private final Logger logger = LoggerFactory.getLogger(getClass());
  
  private final RestTemplate rest = new RestTemplate();
  
  public JobStatusWebhookEventListener(JobRepository aJobRepository) {
    jobRepository = aJobRepository;
  }
  
  private void handleEvent (PiperEvent aEvent) {
    String jobId = aEvent.getRequiredString(DSL.JOB_ID);
    Job job = jobRepository.findOne(jobId);
    if(job == null) {
      logger.warn("Unknown job: {}", jobId);
      return;
    }
    List<Accessor> webhooks = job.getWebhooks();
    for(Accessor webhook : webhooks) {
      if(Events.JOB_STATUS.equals(webhook.getRequiredString(DSL.TYPE))) {
        MapObject webhookEvent = new MapObject(webhook.asMap());
        webhookEvent.put(DSL.EVENT,aEvent.asMap());
        logger.debug("Calling webhook {} -> {}",webhook.getRequiredString(DSL.URL),webhookEvent);
        rest.postForObject(webhook.getRequiredString(DSL.URL), webhookEvent, String.class);
      }
    }
  }
  
  @Override
  public void onApplicationEvent (PiperEvent aEvent) {
    if(aEvent.getType().equals(Events.JOB_STATUS)) {
      handleEvent(aEvent);
    }
  }
  
}
