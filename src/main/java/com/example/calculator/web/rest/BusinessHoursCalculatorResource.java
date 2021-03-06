package com.example.calculator.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.example.calculator.domain.BusinessHoursCalculator;
import com.example.calculator.service.BusinessHoursCalculatorService;
import com.example.calculator.web.rest.errors.CustomParameterizedException;
import com.example.calculator.web.rest.util.HeaderUtil;

/**
 * REST controller for managing BusinessHoursCalculator.
 */
@RestController
@RequestMapping("/api")
public class BusinessHoursCalculatorResource {

    private final Logger log = LoggerFactory.getLogger(BusinessHoursCalculatorResource.class);
    
	private static final DateTimeFormatter STARTING_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm", Locale.US);

    @Inject
    private BusinessHoursCalculatorService businessHoursCalculatorService;
    
    /**
     * POST  /business-hours-calculators : Create a new businessHoursCalculator.
     *
     * @param businessHoursCalculator the businessHoursCalculator to create
     * @return the ResponseEntity with status 201 (Created) and with body the new businessHoursCalculator, or with status 400 (Bad Request) if the businessHoursCalculator has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/business-hours-calculators",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<BusinessHoursCalculator> createBusinessHoursCalculator(@Valid @RequestBody BusinessHoursCalculator businessHoursCalculator) throws URISyntaxException {
        log.debug("REST request to save BusinessHoursCalculator : {}", businessHoursCalculator);
        if (businessHoursCalculator.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("businessHoursCalculator", "idexists", "A new businessHoursCalculator cannot already have an ID")).body(null);
        }
        
        try {
        	LocalDateTime.parse(businessHoursCalculator.getStartingDateTime(), STARTING_DATETIME_FORMATTER);
		} catch (DateTimeParseException e) {
			throw new CustomParameterizedException("Wrong format of the starting datetime: " + businessHoursCalculator.getStartingDateTime());		
		}
        
        businessHoursCalculator.setExpectedPickupTime(businessHoursCalculatorService.calculateDeadline(businessHoursCalculator.getTimeInterval(), businessHoursCalculator.getStartingDateTime()));
        businessHoursCalculator.setActualBusinessHours(businessHoursCalculatorService.prepareBusinessHoursData());
        
        BusinessHoursCalculator result = businessHoursCalculatorService.save(businessHoursCalculator);
        return ResponseEntity.created(new URI("/api/business-hours-calculators/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("businessHoursCalculator", result.getId().toString()))
            .body(result);
    }

	/**
     * PUT  /business-hours-calculators : Updates an existing businessHoursCalculator.
     *
     * @param businessHoursCalculator the businessHoursCalculator to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated businessHoursCalculator,
     * or with status 400 (Bad Request) if the businessHoursCalculator is not valid,
     * or with status 500 (Internal Server Error) if the businessHoursCalculator couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/business-hours-calculators",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<BusinessHoursCalculator> updateBusinessHoursCalculator(@Valid @RequestBody BusinessHoursCalculator businessHoursCalculator) throws URISyntaxException {
        log.debug("REST request to update BusinessHoursCalculator : {}", businessHoursCalculator);
        if (businessHoursCalculator.getId() == null) {
            return createBusinessHoursCalculator(businessHoursCalculator);
        }
        BusinessHoursCalculator result = businessHoursCalculatorService.save(businessHoursCalculator);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("businessHoursCalculator", businessHoursCalculator.getId().toString()))
            .body(result);
    }

    /**
     * GET  /business-hours-calculators : get all the businessHoursCalculators.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of businessHoursCalculators in body
     */
    @RequestMapping(value = "/business-hours-calculators",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<BusinessHoursCalculator> getAllBusinessHoursCalculators() {
        log.debug("REST request to get all BusinessHoursCalculators");
        List<BusinessHoursCalculator> businessHoursCalculators = businessHoursCalculatorService.findAll();
        List<BusinessHoursCalculator> formattedBusinessHoursCalculators = new ArrayList<>();
        
        for (BusinessHoursCalculator businessHoursCalculator : businessHoursCalculators) {
			String formattedActualBusinessHours = formatActualBusinessHours(businessHoursCalculator.getActualBusinessHours());
			businessHoursCalculator.setActualBusinessHours(formattedActualBusinessHours);
			formattedBusinessHoursCalculators.add(businessHoursCalculator);
		}
        
        return formattedBusinessHoursCalculators;
    }

    private String formatActualBusinessHours(String actualBusinessHours) {
		String[] splittedBusinessHours = actualBusinessHours.split("_");
		
		StringBuilder sb = new StringBuilder();
		
		for (String string : splittedBusinessHours) {
			sb.append(string + "\n\n");
		}
		
		return sb.toString();
	}

	/**
     * GET  /business-hours-calculators/:id : get the "id" businessHoursCalculator.
     *
     * @param id the id of the businessHoursCalculator to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the businessHoursCalculator, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/business-hours-calculators/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<BusinessHoursCalculator> getBusinessHoursCalculator(@PathVariable Long id) {
        log.debug("REST request to get BusinessHoursCalculator : {}", id);
        BusinessHoursCalculator businessHoursCalculator = businessHoursCalculatorService.findOne(id);
        return Optional.ofNullable(businessHoursCalculator)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /business-hours-calculators/:id : delete the "id" businessHoursCalculator.
     *
     * @param id the id of the businessHoursCalculator to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/business-hours-calculators/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteBusinessHoursCalculator(@PathVariable Long id) {
        log.debug("REST request to delete BusinessHoursCalculator : {}", id);
        businessHoursCalculatorService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("businessHoursCalculator", id.toString())).build();
    }

}
