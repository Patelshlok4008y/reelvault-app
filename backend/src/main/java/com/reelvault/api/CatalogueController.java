package com.reelvault.api;

import com.reelvault.domain.CatalogueCategory;
import com.reelvault.dto.CataloguePage;
import com.reelvault.dto.CatalogueTitle;
import com.reelvault.service.CatalogueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogue")
public class CatalogueController {
    private final CatalogueService catalogue;
    public CatalogueController(CatalogueService catalogue) { this.catalogue = catalogue; }
    @GetMapping("/{category}")
    public CataloguePage search(@PathVariable CatalogueCategory category, @RequestParam(required = false) String search,
                                @RequestParam(defaultValue = "1") int page, @RequestParam(required = false) String provider) {
        return catalogue.search(provider, category, search, page);
    }
    @GetMapping("/{category}/{id}")
    public CatalogueTitle details(@PathVariable CatalogueCategory category, @PathVariable String id,
                                  @RequestParam(required = false) String provider) {
        return catalogue.details(provider, category, id);
    }
}
