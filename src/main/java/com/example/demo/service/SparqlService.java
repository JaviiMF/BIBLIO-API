package com.example.demo.service;

import org.apache.jena.query.*;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SparqlService {

  private HttpClient createTrustingHttpClient() {
    try {
      TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
          }
      };

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

      return HttpClient.newBuilder()
          .sslContext(sslContext)
          .connectTimeout(java.time.Duration.ofSeconds(60))
          .build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create trusting HTTP client", e);
    }
  }

  public List<Map<String, String>> executeSelectQuery(String endpointUrl, String sparqlQuery) {
    List<Map<String, String>> resultsList = new ArrayList<>();

    try (QueryExecution qexec = QueryExecutionHTTP.service(endpointUrl)
        .query(sparqlQuery)
        .httpClient(createTrustingHttpClient())
        .acceptHeader("application/sparql-results+json")
        .httpHeader("User-Agent", "Biblio-API/1.0")
        .build()) {
      ResultSet results = qexec.execSelect();
      while (results.hasNext()) {
        QuerySolution soln = results.nextSolution();
        Map<String, String> row = new HashMap<>();
        soln.varNames().forEachRemaining(varName -> {
          if (soln.get(varName) != null) {
            row.put(varName, soln.get(varName).toString());
          } else {
            row.put(varName, "");
          }
        });
        resultsList.add(row);
      }
    } catch (Exception e) {
      System.err.println("SPARQL Query Error:");
      System.err.println("Endpoint: " + endpointUrl);
      System.err.println("Query: " + sparqlQuery);
      e.printStackTrace();
      throw new RuntimeException("Error executing SPARQL query: " + e.getMessage(), e);
    }

    return resultsList;
  }

  public String buildQueryFromTopic(String repository, String language, String topic, String limit) {
    if (repository.contains("wikidata")) {
      return """
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX wikibase: <http://wikiba.se/ontology#>
          PREFIX bd: <http://www.bigdata.com/rdf#>
          PREFIX mwapi: <https://www.mediawiki.org/ontology#API/>
          PREFIX wdt: <http://www.wikidata.org/prop/direct/>
          PREFIX schema: <http://schema.org/>
          SELECT ?item ?label ?type ?typeUri ?description ?image ?date ?coords WHERE {
            {
              SELECT DISTINCT ?item WHERE {
                SERVICE wikibase:mwapi {
                  bd:serviceParam wikibase:endpoint "www.wikidata.org" .
                  bd:serviceParam wikibase:api "EntitySearch" .
                  bd:serviceParam mwapi:search "%s" .
                  bd:serviceParam mwapi:language "%s" .
                  ?item wikibase:apiOutputItem mwapi:item .
                }
              } LIMIT %s
            }

            OPTIONAL { ?item wdt:P18|wdt:P154 ?image . }
            OPTIONAL { ?item wdt:P577 ?date . }
            OPTIONAL { ?item wdt:P625 ?coords . }
            OPTIONAL { ?item wdt:P31 ?typeUri . }

            SERVICE wikibase:label {
              bd:serviceParam wikibase:language "%s,en".
              ?item rdfs:label ?label .
              ?item schema:description ?description .
              ?typeUri rdfs:label ?type .
            }
          }""".formatted(topic, language, limit, language);
    } else {
      return """
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
          PREFIX dc: <http://purl.org/dc/elements/1.1/>
          PREFIX schema: <http://schema.org/>
          SELECT DISTINCT ?source ?label ?type ?image ?date ?description WHERE {
            ?source rdfs:label|skos:prefLabel ?label .
            FILTER(CONTAINS(LCASE(STR(?label)), LCASE("%s")))

            OPTIONAL { ?source a ?type . }
            OPTIONAL { ?source dc:description|schema:description ?description . }
            OPTIONAL { ?source schema:image ?image . }
            OPTIONAL { ?source dc:date|schema:datePublished ?date . }
          } LIMIT %s""".formatted(topic, limit);
    }
  }

  public List<Map<String, String>> executeFederatedQuery(String wikidataEntity, String targetRepository) {
    String targetQuery = "";
    String wikidataSparqlEndpoint = "https://query.wikidata.org/sparql";

    if (targetRepository.contains("bne.es")) {
      // TODO NOT WORKING AT THE MOMENT
      // https://www.wikidata.org/wiki/Wikidata:SPARQL_query_service/Federation_report
      // SERVICE DOWN
      throw new RuntimeException("Service not available for entity: " + wikidataEntity);

    } else if (targetRepository.contains("bnf.fr")) {
      targetQuery = getBnfFederatedQuery(wikidataEntity);
      return executeSelectQuery(wikidataSparqlEndpoint, targetQuery);

    } else if (targetRepository.contains("cervantesvirtual")) {
      targetQuery = getBvmcFederatedQuery(wikidataEntity);
      return executeSelectQuery(wikidataSparqlEndpoint, targetQuery);
    } else {
      throw new IllegalArgumentException("Repositorio desconocido: " + targetRepository);
    }
  }

  public String getGoldenAgeAuthorQualityQuery(String countryQid) {
    return """
          PREFIX wdt: <http://www.wikidata.org/prop/direct/>
          PREFIX wd: <http://www.wikidata.org/entity/>
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX wikibase: <http://wikiba.se/ontology#>
          PREFIX bd: <http://www.bigdata.com/rdf#>
  
          SELECT DISTINCT ?author ?bne ?bnf ?bvmc WHERE {
            VALUES ?occupation {
              wd:Q36180   # Escritor
              wd:Q214917  # Dramaturgo
              wd:Q49757   # Poeta
              wd:Q6625963 # Novelista
              wd:Q822146  # Letrista
              wd:Q482980  # Autor
            }
  
            ?author wdt:P106 ?occupation ;
                    wdt:P569 ?birthDate ;
                    wdt:P19  ?lugarNacimiento .
  
            ?lugarNacimiento wdt:P17 %s .
  
            FILTER(YEAR(?birthDate) >= 1492 && YEAR(?birthDate) <= 1681)
  
            OPTIONAL { ?author wdt:P950  ?bne . }
            OPTIONAL { ?author wdt:P268  ?bnf . }
            OPTIONAL { ?author wdt:P2799 ?bvmc . }
          }
          LIMIT 100
          """.formatted(countryQid);
  }

  public String getSilverAgeAuthorQualityQuery(String countryQid) {
    return """
          PREFIX wdt: <http://www.wikidata.org/prop/direct/>
          PREFIX wd: <http://www.wikidata.org/entity/>
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX wikibase: <http://wikiba.se/ontology#>
          PREFIX bd: <http://www.bigdata.com/rdf#>
  
          SELECT DISTINCT ?author ?bne ?bnf ?bvmc WHERE {
            VALUES ?occupation {
              wd:Q36180   # Escritor
              wd:Q214917  # Dramaturgo
              wd:Q49757   # Poeta
              wd:Q6625963 # Novelista
              wd:Q822146  # Letrista
              wd:Q482980  # Autor
            }
  
            ?author wdt:P106 ?occupation ;
                    wdt:P569 ?birthDate ;
                    wdt:P19  ?lugarNacimiento .
  
            ?lugarNacimiento wdt:P17 %s .
  
            FILTER(YEAR(?birthDate) >= 1868 && YEAR(?birthDate) <= 1936)
  
            OPTIONAL { ?author wdt:P950  ?bne . }
            OPTIONAL { ?author wdt:P268  ?bnf . }
            OPTIONAL { ?author wdt:P2799 ?bvmc . }
          }
          LIMIT 100
          """.formatted(countryQid);
  }

  public String getCivilWarAuthorQualityQuery(String countryQid) {
    return """
          PREFIX wdt: <http://www.wikidata.org/prop/direct/>
          PREFIX wd: <http://www.wikidata.org/entity/>
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX wikibase: <http://wikiba.se/ontology#>
          PREFIX bd: <http://www.bigdata.com/rdf#>
  
          SELECT DISTINCT ?author ?bne ?bnf ?bvmc WHERE {
            VALUES ?occupation {
              wd:Q36180   # Escritor
              wd:Q214917  # Dramaturgo
              wd:Q49757   # Poeta
              wd:Q6625963 # Novelista
              wd:Q822146  # Letrista
              wd:Q482980  # Autor
            }
  
            ?author wdt:P106 ?occupation ;
                    wdt:P569 ?birthDate ;
                    wdt:P19  ?lugarNacimiento .
  
            ?lugarNacimiento wdt:P17 %s .
  
            FILTER(YEAR(?birthDate) >= 1931 && YEAR(?birthDate) <= 1939)
  
            OPTIONAL { ?author wdt:P950  ?bne . }
            OPTIONAL { ?author wdt:P268  ?bnf . }
            OPTIONAL { ?author wdt:P2799 ?bvmc . }
          }
          LIMIT 100
          """.formatted(countryQid);
  }

  public String getBnfFederatedQuery(String wikidataQid) {
    return """
        PREFIX dcterms: <http://purl.org/dc/terms/>
        PREFIX rdarelationships: <http://rdvocab.info/RDARelationshipsWEMI/>
        PREFIX rdagroup1elements: <http://rdvocab.info/Elements/>
        PREFIX wdt: <http://www.wikidata.org/prop/direct/>
        PREFIX wd: <http://www.wikidata.org/entity/>

        SELECT DISTINCT ?author ?title ?edition ?placeOfPublication ?yearOfPublication  ?langCode
        WHERE {
          wd:%s wdt:P268 ?id .

          BIND(IRI(CONCAT('http://data.bnf.fr/ark:/12148/cb', ?id, '#about')) AS ?author)

          SERVICE <http://data.bnf.fr/sparql> {
            ?expression <http://id.loc.gov/vocabulary/relators/aut> ?author .

            OPTIONAL {
              ?expression dcterms:language ?langUri .
              BIND(REPLACE(STR(?langUri), "^.*/", "") AS ?langCode)
            }

            ?manifestation rdarelationships:expressionManifested ?expression ;
                           dcterms:title ?title ;
                           dcterms:date ?yearOfPublication .

            OPTIONAL { ?manifestation dcterms:publisher ?edition . }
            OPTIONAL { ?manifestation rdagroup1elements:placeOfPublication ?placeOfPublication . }
          }
        }
        ORDER BY DESC(?yearOfPublication)
        LIMIT 100
        """.formatted(wikidataQid);
  }

  public String getBvmcFederatedQuery(String wikidataQid) {
    return """
        PREFIX rdaw: <http://rdaregistry.info/Elements/w/>
        PREFIX rdam: <http://rdaregistry.info/Elements/m/>
        PREFIX rdae: <http://rdaregistry.info/Elements/e/>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX madsrdf: <http://www.loc.gov/mads/rdf/v1#>
        PREFIX wdt: <http://www.wikidata.org/prop/direct/>
        PREFIX wd: <http://www.wikidata.org/entity/>

        SELECT distinct ?author ?work ?workLabel ?placeOfProduction ?yearOfPublication ?langCode
        WHERE {
          wd:%s wdt:P2799 ?id .
          wd:%s rdfs:label ?author.  FILTER(LANG(?author) = "en").
          BIND(uri(concat("https://data.cervantesvirtual.com/person/", ?id)) as ?bvmcID)
          SERVICE <http://data.cervantesvirtual.com/openrdf-sesame/repositories/data> {
            ?work rdaw:author ?bvmcID .
            ?work rdfs:label ?workLabel .
            ?work rdaw:manifestationOfWork ?manifestation .
            ?work rdaw:expressionOfWork ?expression .
            OPTIONAL {?expression rdae:languageOfExpression ?language . ?language madsrdf:code ?langCode .}
            OPTIONAL {?manifestation rdam:placeOfProduction ?placeOfProduction .}
            OPTIONAL {?manifestation rdam:dateOfPublication ?dateOfPublication . BIND(REPLACE(str(?dateOfPublication), "https://data.cervantesvirtual.com/date/", "", "i") AS ?yearOfPublication) .}
          }
        }
        LIMIT 100
        """
        .formatted(wikidataQid, wikidataQid);
  }
}
