package com.example.demo.controller;

import com.example.demo.service.BusquedaService;
import com.example.demo.service.DataEnrichmentService;
import com.example.demo.service.SparqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.*;
import java.util.Set;

@Controller
public class SearchController {

    private final SparqlService sparqlService;
    private final DataEnrichmentService dataEnrichmentService;
    private final BusquedaService busquedaService;

    @Autowired
    public SearchController(SparqlService sparqlService, DataEnrichmentService dataEnrichmentService,
            BusquedaService busquedaService) {
        this.sparqlService = sparqlService;
        this.dataEnrichmentService = dataEnrichmentService;
        this.busquedaService = busquedaService;
    }

    @GetMapping("/search")
    public String search() {
        return "search";
    }

    @PostMapping("/search")
    public String performSearch(@RequestParam(required = false) String topic,
            @RequestParam(required = false) String sparql,
            @RequestParam String repository,
            @RequestParam String language,
            @RequestParam String limit,
            Model model) {

        String queryToExecute = sparql;
        if (queryToExecute == null || queryToExecute.trim().isEmpty()) {
            if (topic == null || topic.trim().isEmpty()) {
                model.addAttribute("error", "Debes ingresar un tema o una query SPARQL.");
                return "search";
            }
            if (limit == null || limit.trim().isEmpty()) {
                limit = "20";
            }
            if (language == null || language.trim().isEmpty()) {
                language = "es";
            }
            queryToExecute = sparqlService.buildQueryFromTopic(repository, language, topic.trim(), limit);
        }

        long startTime = System.currentTimeMillis();
        try {
            List<Map<String, String>> results = sparqlService.executeSelectQuery(repository, queryToExecute);
            long endTime = System.currentTimeMillis();

            busquedaService.saveBusqueda(topic, repository, queryToExecute, results, endTime, startTime);

            Map<String, Object> enrichedData = dataEnrichmentService.enrichResults(results);

            model.addAttribute("results", results);
            model.addAttribute("enrichedData", enrichedData);
            if (!results.isEmpty()) {
                Set<String> columns = results.get(0).keySet();
                model.addAttribute("columns", columns);
            }
            model.addAttribute("success", "Búsqueda completada en " + (endTime - startTime) + "ms.");
            model.addAttribute("executedQuery", queryToExecute);

        } catch (Exception e) {
            model.addAttribute("error", "Error ejecutando la consulta: " + e.getMessage());
        }

        model.addAttribute("topic", topic);
        model.addAttribute("sparql", sparql);
        model.addAttribute("selectedRepository", repository);

        return "search";
    }

    @GetMapping("/comparative")
    public String comparativeSearch(Model model) {
        return "quality-comparative-search";
    }

    @PostMapping("/comparative")
    public String performComparativeSearch(@RequestParam String country,
            @RequestParam(required = false, defaultValue = "golden-age") String period,
            Model model) {

        long startTime = System.currentTimeMillis();

        String repository = "https://query.wikidata.org/sparql";
        String c = country.toLowerCase().trim();
        String countryQid = null;

        if (c.isEmpty()) {
            countryQid = "wd:Q29";
        } else if (c.contains("esp")) {
            countryQid = "wd:Q29";
        } else if (c.contains("portu")) {
            countryQid = "wd:Q45";
        } else if (c.contains("fran")) {
            countryQid = "wd:Q142";
        } else if (c.contains("ital")) {
            countryQid = "wd:Q38";
        } else if (c.contains("alem") || c.contains("germ")) {
            countryQid = "wd:Q183";
        } else if (c.contains("mex") || c.contains("méx")) {
            countryQid = "wd:Q96";
        } else if (c.contains("colomb")) {
            countryQid = "wd:Q739";
        } else if (c.contains("peru") || c.contains("perú")) {
            countryQid = "wd:Q419";
        } else if (c.contains("argen")) {
            countryQid = "wd:Q414";
        } else if (c.startsWith("wd:q")) {
            countryQid = country;
        }

        String query;
        if ("silver-age".equals(period)) {
            query = sparqlService.getSilverAgeAuthorQualityQuery(countryQid);
        } else if ("civil-war".equals(period)) {
            query = sparqlService.getCivilWarAuthorQualityQuery(countryQid);
        } else {
            query = sparqlService.getGoldenAgeAuthorQualityQuery(countryQid);
        }

        List<Map<String, String>> results = sparqlService.executeSelectQuery(repository, query);

        long endTime = System.currentTimeMillis();

        busquedaService.saveBusqueda(null, repository, query, results, endTime, startTime);

        int totalAuthors = results.size();
        long countBne = results.stream().filter(r -> r.get("bne") != null && !r.get("bne").isEmpty()).count();
        long countBnf = results.stream().filter(r -> r.get("bnf") != null && !r.get("bnf").isEmpty()).count();
        long countBvmc = results.stream().filter(r -> r.get("bvmc") != null && !r.get("bvmc").isEmpty()).count();

        model.addAttribute("results", results);
        model.addAttribute("country", country);
        model.addAttribute("period", period);
        model.addAttribute("totalAuthors", totalAuthors);
        model.addAttribute("countBne", countBne);
        model.addAttribute("countBnf", countBnf);
        model.addAttribute("countBvmc", countBvmc);
        model.addAttribute("totalTime", (endTime - startTime));

        return "quality-comparative-search";
    }

    @GetMapping("/federated")
    public String federatedSearch(Model model) {
        return "federated-search";
    }

    @PostMapping("/federated")
    public String performFederatedSearch(@RequestParam String wikidataEntity,
            @RequestParam String targetRepository,
            Model model) {
        long startTime = System.currentTimeMillis();

        try {
            if (wikidataEntity == null || wikidataEntity.trim().isEmpty()) {
                throw new IllegalArgumentException("La entidad de Wikidata es obligatoria");
            }

            model.addAttribute("wikidataEntity", wikidataEntity);
            model.addAttribute("targetRepository", targetRepository);

            List<Map<String, String>> results = sparqlService.executeFederatedQuery(wikidataEntity.trim(),
                    targetRepository);

            long endTime = System.currentTimeMillis();

            model.addAttribute("results", results);
            model.addAttribute("totalTime", (endTime - startTime));

            if (!results.isEmpty()) {
                List<String> columns = new ArrayList<>();
                if (targetRepository.contains("bnf.fr")) {
                    columns = Arrays.asList("author", "title", "edition", "placeOfPublication",
                            "yearOfPublication", "langCode");
                } else if (targetRepository.contains("cervantesvirtual")) {
                    columns = Arrays.asList("author", "work", "workLabel", "placeOfProduction",
                            "yearOfPublication", "langCode");
                } else {

                }

                model.addAttribute("columns", columns);
            }

        } catch (Exception e) {
            model.addAttribute("error", "Error en búsqueda federada: " + e.getMessage());
            e.printStackTrace();
        }

        return "federated-search";
    }
}
