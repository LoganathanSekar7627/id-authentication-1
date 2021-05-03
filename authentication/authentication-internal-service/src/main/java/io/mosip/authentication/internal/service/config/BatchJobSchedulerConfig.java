package io.mosip.authentication.internal.service.config;

import static io.mosip.authentication.core.constant.IdAuthConfigKeyConstants.CREDENTIAL_STORE_JOB_DELAY;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.kernel.core.logger.spi.Logger;

@Configuration
public class BatchJobSchedulerConfig {
	
	/** The logger. */
	private static Logger logger = IdaLogger.getLogger(BatchJobSchedulerConfig.class);
	
	/** The Constant CREDENTIAL_STORE_DEFAULT_DELAY_MILLISECS_STRING. */
	private static final String CREDENTIAL_STORE_DEFAULT_DELAY_MILLISECS_STRING = "1000";
	
	@Autowired
	@Qualifier("credentialStoreJob")
	private Job credentialStoreJob;
	
	@Autowired
	@Qualifier("retriggerMissingCredentialIssuancesJob")
	private Job retriggerMissingCredentialIssuancesJob;
	
	/** The job launcher. */
	@Autowired
	private JobLauncher jobLauncher;
	
	@Scheduled(fixedDelayString = "${" + CREDENTIAL_STORE_JOB_DELAY + ":" + CREDENTIAL_STORE_DEFAULT_DELAY_MILLISECS_STRING + "}")
	public void scheduleCredentialStoreJob() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(credentialStoreJob, jobParameters);
		} catch (Exception e) {
			logger.error("unable to launch job for credential store batch: {}", e.getMessage(), e);
		}
	}
	
	@Scheduled(fixedDelay=Long.MAX_VALUE)
	public void scheduleMissingCredentialRetriggerJob() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(retriggerMissingCredentialIssuancesJob, jobParameters);
		} catch (Exception e) {
			logger.error("unable to launch job for credential store batch: {}", e.getMessage(), e);
		}
	}


}
