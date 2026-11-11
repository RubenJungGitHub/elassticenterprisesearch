package contain.opensource.enterprisesearch.controllers;

import contain.opensource.enterprisesearch.model.EnterpriseSearchDocument;
import contain.opensource.enterprisesearch.services.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/")
    public String searchPage(@RequestParam(name = "q", required = false) String query, Model model) {
        List<EnterpriseSearchDocument> results = searchService.searchDocuments(query);
        
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("results", results);
        
        return "search"; // Zoekt naar search.html in templates
    }
}