package com.sas.jm.dms.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sas.jm.dms.entity.SparesEvents;
import com.sas.jm.dms.entity.SparesInventory;
import com.sas.jm.dms.repository.SparesEventsRepository;
import com.sas.jm.dms.repository.SparesInventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

	private final SparesEventsRepository sparesEventsRepository;
	private final SparesInventoryRepository sparesInventoryRepository;

	public List<?> findAllSparesEvents() {
		return sparesEventsRepository.findAllByOrderByIdDesc();
	}

	public List<?> deleteSparesEvents(String id) throws Exception {
		SparesEvents events = sparesEventsRepository.findById(id)
				.orElseThrow(() -> new Exception("SparesEvents not found for id: " + id));
		SparesInventory spares = sparesInventoryRepository.findById(events.getSparesId())
				.orElseThrow(() -> new Exception("SparesInventory not found for id: " + events.getSparesId()));
		if (spares.getMinThresh().compareTo(spares.getQty()) > 0) {
			throw new Exception("Event cannot be deleted as spares is still less than the min threshold");
		}
		sparesEventsRepository.deleteById(id);
		return findAllSparesEvents();
	}
}
