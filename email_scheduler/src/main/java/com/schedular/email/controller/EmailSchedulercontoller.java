package com.schedular.email.controller;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import javax.validation.Valid;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schedular.email.job.EmailJob;
import com.schedular.email.payload.EmailRequest;
import com.schedular.email.payload.EmailResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class EmailSchedulercontoller {
	
	@GetMapping("/get")
	public ResponseEntity<String> hello()
	{
		return ResponseEntity.ok("Test get api");
	}
	
	@Autowired
	private Scheduler scheduler;
	
	@RequestMapping("/schedule/email")
	public ResponseEntity<EmailResponse> scheduleEmail(@Valid @RequestBody EmailRequest emailRequest){
		try {
			
			ZonedDateTime dateTime=ZonedDateTime.of(emailRequest.getDateTime(), emailRequest.getTimeZone());
			
			if(dateTime.isBefore(ZonedDateTime.now())) {
				EmailResponse emailResponse=new EmailResponse(false, "date time must be after current time");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(emailResponse);
				
			}
			
			JobDetail jobDetail=buildJobDetail(emailRequest);
			Trigger trigger=buiTrigger(jobDetail, dateTime);
			
			scheduler.scheduleJob(jobDetail,trigger);
			
			EmailResponse emailResponse=new EmailResponse(true, jobDetail.getKey().getName(),
					jobDetail.getKey().getGroup(), "Email Scheduled SuccessFully");
			
			return ResponseEntity.ok(emailResponse);
			
			
			
		}catch(SchedulerException schedulerException)
		{
			log.error("error while scheduling email",schedulerException);
			EmailResponse emailResponse=new EmailResponse(false,"error while scheduling email. try again later");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(emailResponse);
			
		}
	}
	
	
	
	
	
	
	private  JobDetail buildJobDetail(EmailRequest scheduleEmailRequest) {
		JobDataMap jobDataMap=new JobDataMap();
		
		jobDataMap.put("email", scheduleEmailRequest.getEmail());
		jobDataMap.put("subject", scheduleEmailRequest.getSubject());
		jobDataMap.put("body", scheduleEmailRequest.getBody());
		
		return JobBuilder.newJob(EmailJob.class)
				.withIdentity(UUID.randomUUID().toString(),"email-job")
				.withDescription("send email job")
				.usingJobData(jobDataMap)
				.storeDurably()
				.build();
		
	}
	
	private Trigger buiTrigger(JobDetail jobDetail,ZonedDateTime startAt) {
		
		return TriggerBuilder.newTrigger()
				.forJob(jobDetail)
				.withIdentity(jobDetail.getKey().getName(),"email-trigger")
				.withDescription("email Trigger")
				.startAt(Date.from(startAt.toInstant()))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
				.build();
	}
	

}
