package com.ftn.uns.travelplanerbackend.controller;

import java.util.List;
import java.util.Optional;

import com.ftn.uns.travelplanerbackend.model.User;
import com.ftn.uns.travelplanerbackend.repository.LocationRepository;
import com.ftn.uns.travelplanerbackend.service.*;
import com.ftn.uns.travelplanerbackend.utils.GoogleCoordinatesService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ftn.uns.travelplanerbackend.model.Location;
import com.ftn.uns.travelplanerbackend.model.Object;
import com.ftn.uns.travelplanerbackend.model.Transportation;
import com.ftn.uns.travelplanerbackend.model.Travel;
import com.ftn.uns.travelplanerbackend.model.dto.Converters;
import com.ftn.uns.travelplanerbackend.model.dto.TravelDTO;

@RestController
@RequestMapping(value="travels")
public class TravelController {

	@Autowired
	private TravelService travelService;
	@Autowired
	private LocationService locationService;
	@Autowired
	private LocationRepository locationRepository;
	@Autowired
	private UserService userService;
	@Autowired
	private ObjectService objectService;
	@Autowired
	private TransportationService transportationService;

	@PreAuthorize("isAuthenticated()")
	@RequestMapping
	public ResponseEntity<List<Travel>> getAllTravels(Authentication authentication) {
		User user = userService.findOne(Long.valueOf(authentication.getName()));
		List<Travel> travels = user.getTravels();
		return new ResponseEntity<List<Travel>>(travels, HttpStatus.OK);
	}

	@PreAuthorize("isAuthenticated()")
	@RequestMapping(value="{id}")
	public ResponseEntity<TravelDTO> getTravel(@PathVariable("id") Long id) {
		Travel travel = travelService.findOne(id);
		TravelDTO dto = Converters.convertTravelToDTO(travel);
		System.out.println("\nSystem.out.println(travel.getId());");
		System.out.println(travel.getId());
		System.out.println(travel.getCurrency());
		return new ResponseEntity<TravelDTO>(dto, HttpStatus.OK);
	}
	
	@RequestMapping(method=RequestMethod.POST, consumes="application/json")
	public ResponseEntity<Travel> addTravel(@RequestBody Travel travel, Authentication authentication) {
		Object object = objectService.save(travel.getAccommodation());
		travel.setAccommodation(object);
		Transportation origin = travel.getOrigin();
		origin.setLocation(getProperLocation(origin));
		origin = transportationService.save(origin);
		travel.setOrigin(origin);
		Transportation destination = travel.getDestination();
		destination.setLocation(getProperLocation(destination));
		destination = transportationService.save(destination);
		travel.setDestination(destination);
		Travel newTravel = travelService.save(travel);
		User user = userService.findOne(Long.valueOf(authentication.getName()));
		user.addTravel(newTravel);
		userService.save(user);
		return new ResponseEntity<Travel>(newTravel, HttpStatus.CREATED);
	}
	
	Location getProperLocation(Transportation transportation) {
		Location transportationLocation = transportation.getLocation();
		Optional<Location> optionalTransportationLocation = locationRepository.findByCityAndCountry(transportationLocation.getCity(), transportationLocation.getCountry());
		Location persistentTransportationLocation;

		if (optionalTransportationLocation.isPresent()) {
			persistentTransportationLocation = optionalTransportationLocation.get();
		}
		else {
			GoogleCoordinatesService googleCoordinatesService = new GoogleCoordinatesService();
			persistentTransportationLocation = locationService.save(googleCoordinatesService.getCoordinatesFromAddress(transportationLocation.getCity(), transportationLocation.getCountry()));
		}
		return persistentTransportationLocation;
	}
	
	@PreAuthorize("isAuthenticated()")
	@RequestMapping(method=RequestMethod.DELETE, value="/{id}")
	public ResponseEntity<Travel> deleteTravel(@PathVariable("id") Long id, Authentication authentication) {
		User user = userService.findOne(Long.valueOf(authentication.getName()));
		Travel deletedTravel = travelService.findOne(id);
		user.getTravels().remove(deletedTravel);
		userService.save(user);
		deletedTravel = travelService.delete(id);
		return new ResponseEntity<Travel>(deletedTravel, HttpStatus.NO_CONTENT);
	}
	
	@RequestMapping(method=RequestMethod.PUT, value="/{id}", consumes="application/json", produces="application/json")
	public ResponseEntity<Travel> editTravel(@PathVariable("id") Long id, @RequestBody Travel travel) {
		Travel editedTravel = travelService.findOne(id);
		editedTravel.setCurrency(travel.getCurrency());
		travelService.save(editedTravel);
		return new ResponseEntity<Travel>(editedTravel, HttpStatus.OK);
	}
}
