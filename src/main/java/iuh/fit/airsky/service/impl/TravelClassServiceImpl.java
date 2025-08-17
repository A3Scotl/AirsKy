package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.TravelClassRequest;
import iuh.fit.airsky.dto.response.TravelClassResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.TravelClassMapper;
import iuh.fit.airsky.model.TravelClass;
import iuh.fit.airsky.repository.TravelClassRepository;
import iuh.fit.airsky.service.TravelClassService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class TravelClassServiceImpl implements TravelClassService {

    private final TravelClassRepository travelClassRepository;
    private final TravelClassMapper travelClassMapper;

    public TravelClassServiceImpl(TravelClassRepository travelClassRepository, TravelClassMapper travelClassMapper) {
        this.travelClassRepository = travelClassRepository;
        this.travelClassMapper = travelClassMapper;
    }

    @Override
    public TravelClassResponse createTravelClass(TravelClassRequest request) {
        log.info("Creating new travel class with name: {}", request.getClassName());
        TravelClass travelClass = travelClassMapper.toEntity(request);
        TravelClass saved = travelClassRepository.save(travelClass);
        log.info("Travel class created with ID: {}", saved.getClassId());
        return travelClassMapper.toResponseDTO(saved);
    }

    @Override
    public TravelClassResponse updateTravelClass(Long id, TravelClassRequest request) {
        log.info("Updating travel class with ID: {}", id);
        TravelClass travelClass = travelClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TravelClass not found with id " + id));
        travelClass.setClassName(request.getClassName());
        travelClass.setBenefits(request.getBenefits());
        travelClass.setPriceMultiplier(request.getPriceMultiplier());
        TravelClass updated = travelClassRepository.save(travelClass);
        log.info("Travel class updated with ID: {}", updated.getClassId());
        return travelClassMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<TravelClassResponse> findById(Long id) {
        log.info("Finding travel class by ID: {}", id);
        return travelClassRepository.findById(id).map(travelClassMapper::toResponseDTO);
    }

    @Override
    public PageResponse<TravelClassResponse> findAll(Pageable pageable) {
        log.info("Finding all travel classes with pagination: {}", pageable);
        Page<TravelClass> page = travelClassRepository.findAll(pageable);
        return new PageResponse<>(page.map(travelClassMapper::toResponseDTO));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting travel class with ID: {}", id);
        if (!travelClassRepository.existsById(id)) {
            log.warn("TravelClass not found for delete: {}", id);
            throw new ResourceNotFoundException("TravelClass not found with id " + id);
        }
        travelClassRepository.deleteById(id);
        log.info("Travel class deleted: {}", id);
    }
}