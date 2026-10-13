package com.reelvault.api;

import com.reelvault.domain.MediaType;
import com.reelvault.domain.WatchStatus;
import com.reelvault.dto.MediaRequest;
import com.reelvault.dto.MediaResponse;
import com.reelvault.dto.ImportRequest;
import com.reelvault.service.CatalogueService;
import com.reelvault.dto.PersonalUpdateRequest;
import com.reelvault.dto.StatusUpdateRequest;
import com.reelvault.service.LibraryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final LibraryService libraryService;
    private final CatalogueService catalogueService;

    public MediaController(LibraryService libraryService, CatalogueService catalogueService) {
        this.libraryService = libraryService;
        this.catalogueService = catalogueService;
    }

    @GetMapping
    public List<MediaResponse> findMedia(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MediaType type,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) WatchStatus status,
            @RequestParam(defaultValue = "recent") String sort) {
        return libraryService.findMedia(search, type, genre, status, sort);
    }

    @GetMapping("/{id}")
    public MediaResponse getMedia(@PathVariable long id) {
        return libraryService.getMedia(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse createMedia(@Valid @RequestBody MediaRequest request) {
        return libraryService.createMedia(request);
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse importCatalogueTitle(@Valid @RequestBody ImportRequest request) {
        return libraryService.importCatalogueTitle(catalogueService.details(request.provider(), request.category(), request.externalId()));
    }

    @PutMapping("/{id}")
    public MediaResponse updateMedia(@PathVariable long id, @Valid @RequestBody MediaRequest request) {
        return libraryService.updateMedia(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@PathVariable long id) {
        libraryService.deleteMedia(id);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/status")
    public MediaResponse updateStatus(@PathVariable long id, @Valid @RequestBody StatusUpdateRequest request) {
        return libraryService.updateStatus(id, request.watchStatus());
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/personal")
    public MediaResponse updatePersonalData(@PathVariable long id, @Valid @RequestBody PersonalUpdateRequest request) {
        return libraryService.updatePersonalData(id, request);
    }
}
