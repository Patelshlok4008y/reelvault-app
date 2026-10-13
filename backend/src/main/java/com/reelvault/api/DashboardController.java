package com.reelvault.api;

import com.reelvault.dto.DashboardResponse;
import com.reelvault.service.LibraryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final LibraryService libraryService;

    public DashboardController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    public DashboardResponse dashboard() {
        return libraryService.getDashboard();
    }
}
