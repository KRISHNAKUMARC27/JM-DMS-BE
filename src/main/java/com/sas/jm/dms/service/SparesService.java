package com.sas.jm.dms.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sas.jm.dms.entity.SparesCategory;
import com.sas.jm.dms.entity.SparesInventory;
import com.sas.jm.dms.model.SparesFilter;
import com.sas.jm.dms.repository.SparesCategoryRepository;
import com.sas.jm.dms.repository.SparesInventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SparesService {

	private final SparesInventoryRepository sparesInventoryRepository;
	private final SparesCategoryRepository sparesCategoryRepository;

	public List<?> findAll() {
		return sparesInventoryRepository.findAllByOrderByIdDesc();
	}

	public SparesInventory save(SparesInventory spares) {
		spares.setUpdateDate(LocalDateTime.now());
		return sparesInventoryRepository.save(spares);
	}

	public List<SparesInventory> findSparesInventoryWithFilter(SparesFilter sparesFilter) {
		return sparesInventoryRepository.findSparesInventoryWithFilter(sparesFilter.categoryList(),
				sparesFilter.desc());
	}

	public List<?> findAllSparesCategory() {
		return sparesCategoryRepository.findAll();
	}

	public SparesCategory saveSparesCategory(SparesCategory sparesCategory) {
		return sparesCategoryRepository.save(sparesCategory);
	}
}
