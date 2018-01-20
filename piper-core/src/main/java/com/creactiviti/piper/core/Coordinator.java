package com.creactiviti.piper.core;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.creactiviti.piper.core.context.ContextRepository;
import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.error.ErrorHandler;
import com.creactiviti.piper.core.error.ErrorObject;
import com.creactiviti.piper.core.error.Errorable;
import com.creactiviti.piper.core.error.Prioritizable;
import com.creactiviti.piper.core.event.EventPublisher;
import com.creactiviti.piper.core.event.Events;
import com.creactiviti.piper.core.event.PiperEvent;
import com.creactiviti.piper.core.job.Job;
import com.creactiviti.piper.core.job.JobRepository;
import com.creactiviti.piper.core.job.JobStatus;
import com.creactiviti.piper.core.job.SimpleJob;
import com.creactiviti.piper.core.messenger.Messenger;
import com.creactiviti.piper.core.messenger.Queues;
import com.creactiviti.piper.core.pipeline.Pipeline;
import com.creactiviti.piper.core.pipeline.PipelineRepository;
import com.creactiviti.piper.core.task.CancelTask;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.TaskDispatcher;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskExecutionRepository;
import com.creactiviti.piper.core.task.TaskStatus;
import com.creactiviti.piper.core.uuid.UUIDGenerator;

/**
 * The central class responsible for coordinating 
 * and executing jobs.
 * 
 * @author Arik Cohen
 * @since Jun 12, 2016
 */
public class Coordinator {

  private PipelineRepository pipelineRepository;
  private JobRepository jobRepository;
  private TaskExecutionRepository jobTaskRepository;
  private EventPublisher eventPublisher;
  private ContextRepository contextRepository;
  private TaskDispatcher taskDispatcher;
  private ErrorHandler errorHandler;
  private TaskCompletionHandler taskCompletionHandler;
  private JobExecutor jobExecutor; 
  private Messenger messenger;
  
  private static final String PIPELINE_ID = "pipelineId";
  private static final String TAGS = "tags";
  private static final String INPUTS = "inputs";
  private static final String WEBHOOKS = "webhooks";

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Starts a job instance.
   * 
   * @param aJobParams
   *          The Key-Value map representing the job
   *          parameters
   * @return Job
   *           The instance of the Job
   */
  public Job create (Map<String,Object> aJobParams) {
    Assert.notNull(aJobParams,"request can't be null");
    MapObject jobParams = MapObject.of(aJobParams);
    String pipelineId = jobParams.getRequiredString(PIPELINE_ID);
    Pipeline pipeline = pipelineRepository.findOne(pipelineId);    
    Assert.notNull(pipeline,String.format("Unkown pipeline: %s", pipelineId));
    Assert.isNull(pipeline.getError(), pipeline.getError()!=null?String.format("%s: %s",pipelineId,pipeline.getError().getMessage()):null);

    validate (jobParams, pipeline);
    
    MapObject inputs = MapObject.of(jobParams.getMap(INPUTS,Collections.EMPTY_MAP));
    List<Accessor> webhooks = jobParams.getList(WEBHOOKS, MapObject.class, Collections.EMPTY_LIST);
    List<String> tags = (List<String>) aJobParams.get(TAGS);

    SimpleJob job = new SimpleJob();
    job.setId(UUIDGenerator.generate());
    job.setLabel(jobParams.getString(DSL.LABEL,pipeline.getLabel()));
    job.setPriority(jobParams.getInteger(DSL.PRIORTIY, Prioritizable.DEFAULT_PRIORITY));
    job.setPipelineId(pipeline.getId());
    job.setStatus(JobStatus.CREATED);
    job.setCreateTime(new Date());
    job.setTags(tags!=null?tags.toArray(new String[tags.size()]):new String[0]);
    job.setWebhooks(webhooks!=null?webhooks:Collections.EMPTY_LIST);
    job.setInputs(inputs);
    log.debug("Job {} started",job.getId());
    jobRepository.create(job);
    
    MapContext context = new MapContext(jobParams.getMap(INPUTS,Collections.EMPTY_MAP));
    contextRepository.push(job.getId(),context);
    
    eventPublisher.publishEvent(PiperEvent.of(Events.JOB_STATUS,"jobId",job.getId(),"status",job.getStatus()));
    
    messenger.send(Queues.JOBS, job);

    return job;
  }

  public void start (Job aJob) {
    SimpleJob job = new SimpleJob(aJob);
    job.setStartTime(new Date());
    job.setStatus(JobStatus.STARTED);
    job.setCurrentTask(0);
    jobRepository.merge(job);
    jobExecutor.execute (job);
    eventPublisher.publishEvent(PiperEvent.of(Events.JOB_STATUS,"jobId",aJob.getId(),"status",job.getStatus()));
  }
  
  private void validate (MapObject aCreateJobParams, Pipeline aPipeline) {
    // validate inputs
    Map<String, Object> inputs = aCreateJobParams.getMap(DSL.INPUTS,Collections.EMPTY_MAP);
    List<Accessor> input = aPipeline.getInputs();
    for(Accessor in : input) {
      if(in.getBoolean(DSL.REQUIRED, false)) {
        Assert.isTrue(inputs.containsKey(in.get(DSL.NAME)), "Missing required param: " + in.get("name"));
      }
    }
    // validate webhooks
    List<Accessor> webhooks = aCreateJobParams.getList(WEBHOOKS, MapObject.class, Collections.EMPTY_LIST);
    for(Accessor webhook : webhooks) {
      Assert.notNull(webhook.getString(DSL.TYPE), "must define 'type' on webhook");
      Assert.notNull(webhook.getString(DSL.URL), "must define 'url' on webhook");
    }
  }

  /**
   * Stop a running job.
   * 
   * @param aJobId
   *          The id of the job to stop
   *          
   * @return The stopped {@link Job}
   */
  public Job stop (String aJobId) {
    Job job = jobRepository.findOne(aJobId);
    Assert.notNull(job,"Unknown job: " + aJobId);
    Assert.isTrue(job.getStatus()==JobStatus.STARTED,"Job " + aJobId + " can not be stopped as it is " + job.getStatus());
    SimpleJob mjob = new SimpleJob(job);
    mjob.setStatus(JobStatus.STOPPED);
    jobRepository.merge(mjob);
    eventPublisher.publishEvent(PiperEvent.of(Events.JOB_STATUS,"jobId",job.getId(),"status",job.getStatus()));
    if(mjob.getExecution().size() > 0) {
      SimpleTaskExecution currentTask = SimpleTaskExecution.createForUpdate(job.getExecution().get(job.getExecution().size()-1));
      currentTask.setStatus(TaskStatus.CANCELLED);
      currentTask.setEndTime(new Date());
      jobTaskRepository.merge(currentTask);
      taskDispatcher.dispatch(new CancelTask(currentTask.getId()));
    }
    return mjob;
  }

  /**
   * Resume a stopped or failed job.
   * 
   * @param aJobId
   *          The id of the job to resume.
   * @return The resumed job
   */
  public Job resume (String aJobId) {
    log.debug("Resuming job {}", aJobId);
    Job job = jobRepository.findOne (aJobId);
    Assert.notNull(job,String.format("Unknown job %s",aJobId));
    Assert.isTrue(isRestartable(job), "can't stop job " + aJobId + " as it is " + job.getStatus());
    SimpleJob mjob = new SimpleJob (job);
    mjob.setStatus(JobStatus.STARTED);
    jobRepository.merge(mjob);
    jobExecutor.execute(mjob);
    return mjob;
  }

  private boolean isRestartable (Job aJob) {
    return aJob.getStatus() == JobStatus.STOPPED || aJob.getStatus() == JobStatus.FAILED;
  }

  /**
   * Complete a task of a given job.
   * 
   * @param aTask
   *          The task to complete.
   */
  public void complete (TaskExecution aTask) {
    try {
      taskCompletionHandler.handle(aTask);
    }
    catch (Exception e) {
      SimpleTaskExecution exec = SimpleTaskExecution.createForUpdate(aTask);
      exec.setError(new ErrorObject(e.getMessage(), ExceptionUtils.getStackFrames(e)));
      handleError(exec);
    }
  }

  /**
   * Handle an application error.
   * 
   * @param aErrorable
   *          The erring message.
   */
  public void handleError (Errorable aErrorable) {
    errorHandler.handle(aErrorable);
  }

  public void setContextRepository(ContextRepository aContextRepository) {
    contextRepository = aContextRepository;
  }

  public void setEventPublisher(EventPublisher aEventPublisher) {
    eventPublisher = aEventPublisher;
  }
  
  public void setJobRepository(JobRepository aJobRepository) {
    jobRepository = aJobRepository;
  }

  public void setTaskDispatcher(TaskDispatcher aTaskDispatcher) {
    taskDispatcher = aTaskDispatcher;
  }

  public void setPipelineRepository(PipelineRepository aPipelineRepository) {
    pipelineRepository = aPipelineRepository;
  }

  public void setJobTaskRepository(TaskExecutionRepository aJobTaskRepository) {
    jobTaskRepository = aJobTaskRepository;
  }

  public void setErrorHandler(ErrorHandler aErrorHandler) {
    errorHandler = aErrorHandler;
  }
  
  public void setTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
    taskCompletionHandler = aTaskCompletionHandler;
  }
  
  public void setJobExecutor(JobExecutor aJobExecutor) {
    jobExecutor = aJobExecutor;
  }
  
  public void setMessenger(Messenger aMessenger) {
    messenger = aMessenger;
  }
  
}
