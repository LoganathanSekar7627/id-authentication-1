package io.mosip.authentication.internal.service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.authentication.core.dto.DataValidationUtil;
import io.mosip.authentication.core.exception.IDDataValidationException;
import io.mosip.authentication.core.exception.IdAuthenticationAppException;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.core.spi.idevent.service.IdChangeEventHandlerService;
import io.mosip.authentication.internal.service.validator.IdEventNotificationValidator;
import io.mosip.idrepository.core.dto.EventDTO;
import io.mosip.idrepository.core.dto.EventsDTO;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.annotations.ApiIgnore;

/**
 * The {@code IdRepoNotificationHandlerController} used to handle the
 * notification events posted by ID Repo module.
 *
 * @author Loganathan Sekar
 */
@RestController
public class IdRepoNotificationHandlerController {
	
	/** The mosipLogger. */
	private Logger mosipLogger = IdaLogger.getLogger(IdRepoNotificationHandlerController.class);
	
	/** The id change event handler service. */
	@Autowired
	private IdChangeEventHandlerService idChangeEventHandlerService;
	
	@Autowired
	private IdEventNotificationValidator validator;
	
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(validator);
	}

	/**
	 * Handle events end point.
	 *
	 * @param notificationEventsDto the notification events dto
	 * @param e the e
	 * @return the response entity
	 * @throws IDDataValidationException 
	 */
	@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR', 'RESIDENT', 'ID_AUTHENTICATION')")
	@PostMapping(path = "/notify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Event Notification Callback API", response = IdAuthenticationAppException.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Request authenticated successfully") })
	public ResponseEntity<?> handleEvents(@Validated @RequestBody RequestWrapper<EventsDTO> notificationEventsDto, @ApiIgnore Errors e) throws IDDataValidationException {
		DataValidationUtil.validate(e);
		EventsDTO request = notificationEventsDto.getRequest();
		if(request != null) {
			List<EventDTO> events = request.getEvents();
			if(events != null && handleEvents(events)) {
					return ResponseEntity.ok().build();
			}
		}
		return ResponseEntity.unprocessableEntity().build();
	}

	/**
	 * Handle events.
	 *
	 * @param events the events
	 * @return true, if successful
	 */
	private boolean handleEvents(List<EventDTO> events) {
		return idChangeEventHandlerService.handleIdEvent(events);
	}
	


	

}
