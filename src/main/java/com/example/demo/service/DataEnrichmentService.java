package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataEnrichmentService {

    /**
     * Metodo que orquesta el enriquecimiento de los resultados obtenidos de la consulta SPARQL ejecutada
     * Agrupa en un Map todos los valores enriquecidos
     * @param results the data from de SPARQL query
     * @return the map with the enriched data
     */
    public Map<String, Object> enrichResults(List<Map<String, String>> results) {
        Map<String, Object> enrichedData = new HashMap<>();

        enrichedData.put("rawData", results);
        enrichedData.put("cardData", enrichForCardView(results));
        enrichedData.put("graphData", enrichForGraphView(results));
        // TODO TIMELINE VIEW NOT IMPLEMENTED YET
        // enrichedData.put("timelineData", enrichForTimelineView(results));
        enrichedData.put("mapData", enrichForMapView(results));
        enrichedData.put("availableViews", detectAvailableViews(results));

        return enrichedData;
    }

    /**
     * Metodo que detecta que vistas deben ser visibles en base a los resultados obtenidos
     * @param results retrieved from de SPARQL query
     * @return available views
     */
    public List<String> detectAvailableViews(List<Map<String, String>> results) {
        List<String> availableViews = new ArrayList<>();

        // Tarjetas y tabla siempre son accesibles da igual la info
        availableViews.add("cards");
        availableViews.add("table");

        if (results.isEmpty()) {
            return availableViews;
        }

        // Se comprueban las relaciones para el modo grafo
        boolean hasRelationships = results.stream()
                .anyMatch(row -> row.keySet().stream()
                        .anyMatch(key -> key.contains("related") || key.contains("type") || key.contains("predicate")));
        if (hasRelationships || results.size() > 1) {
            availableViews.add("graph");
        }

        // Se comprueban fechas para las líneas de tiempo (NO IMPLEMENTADO)
        boolean hasDates = results.stream()
                .anyMatch(row -> row.keySet().stream()
                        .anyMatch(key -> key.toLowerCase().contains("date") ||
                                key.toLowerCase().contains("year") ||
                                key.toLowerCase().contains("inception") ||
                                key.toLowerCase().contains("publication")));
        if (hasDates) {
            availableViews.add("timeline");
        }

        // Se comprueban coordenadas geograficas para el mapa visual
        boolean hasCoords = results.stream()
                .anyMatch(row -> row.keySet().stream()
                        .anyMatch(key -> key.toLowerCase().contains("coord") ||
                                key.toLowerCase().contains("lat") ||
                                key.toLowerCase().contains("lon") ||
                                key.toLowerCase().contains("location")));
        if (hasCoords) {
            availableViews.add("map");
        }

        return availableViews;
    }

    /**
     * Metodo que enriquece la informacion obtenida para el modo tarjeta
     * @param results the data from SPARQL query
     * @return the enriched data
     */
    public List<Map<String, Object>> enrichForCardView(List<Map<String, String>> results) {
        return results.stream().map(row -> {
            Map<String, Object> card = new HashMap<>();

            // Se extraen los campos principales
            String title = extractTitle(row);
            String description = extractDescription(row);
            String image = extractImage(row);
            String type = extractType(row);
            String url = extractUrl(row);

            // Se agrega la informacion al Map
            card.put("title", title);
            card.put("description", description);
            card.put("image", image);
            card.put("type", type);
            card.put("url", url);
            card.put("metadata", extractMetadata(row));

            return card;
        }).collect(Collectors.toList());
    }

    /**
     * Metodo que enriquece la informacion para el modo grafo
     * @param results the data from SPARQL query
     * @return the enriched data
     */
    public Map<String, Object> enrichForGraphView(List<Map<String, String>> results) {
        Map<String, Object> graphData = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> addedNodes = new HashSet<>();

        int edgeId = 0;

        // Se recorren los resultados
        for (Map<String, String> row : results) {
            String nodeId = extractNodeId(row);
            if (!addedNodes.contains(nodeId)) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", nodeId);
                node.put("label", extractTitle(row));
                node.put("title", extractDescription(row));
                node.put("group", extractType(row));
                nodes.add(node);
                addedNodes.add(nodeId);
            }

            // Se comprueba si existen relaciones entre nodos
            for (Map.Entry<String, String> entry : row.entrySet()) {
                String key = entry.getKey().toLowerCase();
                if (key.contains("related") || key.contains("type") || key.contains("predicate")) {
                    String relatedId = entry.getValue();
                    if (relatedId != null && !relatedId.isEmpty() && !relatedId.equals(nodeId)) {
                        // Se agregan los nodos relacioandos
                        if (!addedNodes.contains(relatedId)) {
                            Map<String, Object> relatedNode = new HashMap<>();
                            relatedNode.put("id", relatedId);
                            relatedNode.put("label", extractLabelFromUri(relatedId));
                            relatedNode.put("group", key);
                            nodes.add(relatedNode);
                            addedNodes.add(relatedId);
                        }

                        Map<String, Object> edge = new HashMap<>();
                        edge.put("id", edgeId++);
                        edge.put("from", nodeId);
                        edge.put("to", relatedId);
                        edge.put("label", key);
                        edges.add(edge);
                    }
                }
            }
        }

        graphData.put("nodes", nodes);
        graphData.put("edges", edges);

        return graphData;
    }

    // TODO NOT IMPLEMENTED YET
    // public List<Map<String, Object>> enrichForTimelineView(List<Map<String,
    // String>> results) {
    // List<Map<String, Object>> timelineItems = new ArrayList<>();
    //
    // for (int i = 0; i < results.size(); i++) {
    // Map<String, String> row = results.get(i);
    // String date = extractDate(row);
    //
    // if (date != null && !date.isEmpty()) {
    // Map<String, Object> item = new HashMap<>();
    // item.put("id", i);
    // item.put("content", extractTitle(row));
    // item.put("start", parseDate(date));
    // item.put("title", extractDescription(row));
    // item.put("type", extractType(row));
    // timelineItems.add(item);
    // }
    // }
    //
    // return timelineItems;
    // }


    /**
     * Metodo para enriquecer la información para el mapa de visualizacion
     * @param results the data from SPARQL query
     * @return the enriched data
     */
    public List<Map<String, Object>> enrichForMapView(List<Map<String, String>> results) {
        List<Map<String, Object>> markers = new ArrayList<>();

        // Se recorren los resultados
        for (Map<String, String> row : results) {
            Map<String, Double> coords = extractCoordinates(row);

            // Se comprueba que hayan coordenadas y se rellenan los datos
            if (coords != null) {
                Map<String, Object> marker = new HashMap<>();
                marker.put("lat", coords.get("lat"));
                marker.put("lng", coords.get("lng"));
                marker.put("title", extractTitle(row));
                marker.put("description", extractDescription(row));
                marker.put("type", extractType(row));
                markers.add(marker);
            }
        }

        return markers;
    }

    /**
     * Metodo auxiliar para extraer el titulo
     * @param row the data to extract from
     * @return the tittle
     */
    private String extractTitle(Map<String, String> row) {
        for (String key : Arrays.asList("label", "name", "title", "itemLabel", "sLabel")) {
            if (row.containsKey(key) && row.get(key) != null) {
                return cleanValue(row.get(key));
            }
        }

        return row.values().stream()
                .filter(v -> v != null && !v.startsWith("http"))
                .findFirst()
                .map(this::cleanValue)
                .orElse("Sin título");
    }

    /**
     * Metodo auxiliar para extraer la descripcion
     * @param row the data to extract from
     * @return the description
     */
    private String extractDescription(Map<String, String> row) {
        for (String key : Arrays.asList("description", "abstract", "comment", "descriptionLabel")) {
            if (row.containsKey(key) && row.get(key) != null) {
                return cleanValue(row.get(key));
            }
        }
        return "";
    }

    /**
     * Metodo auxiliar para extraer las imagenes
     * @param row the data to extract from
     * @return the image
     */
    private String extractImage(Map<String, String> row) {
        for (String key : Arrays.asList("image", "img", "picture", "thumbnail", "imageLabel")) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key);
            }
        }
        return null;
    }

    /**
     * Metodo para extraer el tipo
     * @param row the data to extract from
     * @return the type
     */
    private String extractType(Map<String, String> row) {
        for (String key : Arrays.asList("type", "typeLabel", "class", "category")) {
            if (row.containsKey(key) && row.get(key) != null) {
                return extractLabelFromUri(row.get(key));
            }
        }
        return "unknown";
    }

    /**
     * Metodo para extraer las urls
     * @param row the data to extract from
     * @return the url
     */
    private String extractUrl(Map<String, String> row) {
        for (String key : Arrays.asList("item", "s", "subject", "source", "uri")) {
            if (row.containsKey(key) && row.get(key) != null && row.get(key).startsWith("http")) {
                return row.get(key);
            }
        }
        return null;
    }

    /**
     * Meotodo auxiliar para extraer el id del nodo
     * @param row the data to extract from
     * @return the node id
     */
    private String extractNodeId(Map<String, String> row) {
        String url = extractUrl(row);
        return url != null ? url : "node_" + row.hashCode();
    }

    /**
     * Metodo auxiliar para extaer la información
     * @param row
     * @return
     */
    private Map<String, String> extractMetadata(Map<String, String> row) {
        Map<String, String> metadata = new HashMap<>();

        // Add all non-primary fields as metadata
        for (Map.Entry<String, String> entry : row.entrySet()) {
            String key = entry.getKey();
            if (!key.matches("(label|name|title|description|image|type).*")) {
                metadata.put(key, cleanValue(entry.getValue()));
            }
        }

        return metadata;
    }

//    private String extractDate(Map<String, String> row) {
//        for (String key : Arrays.asList("date", "publicationDate", "inception", "year", "dateLabel")) {
//            if (row.containsKey(key) && row.get(key) != null) {
//                return row.get(key);
//            }
//        }
//        return null;
//    }
//
//    private String parseDate(String dateStr) {
//        // Simple date parsing - extract year if possible
//        if (dateStr.matches("\\d{4}.*")) {
//            return dateStr.substring(0, 4);
//        }
//        return dateStr;
//    }

    /**
     * Metodo auxiliar para extaer las coordenadas
     * @param row the data to extract from
     * @return the coordinates
     */
    private Map<String, Double> extractCoordinates(Map<String, String> row) {
        for (String key : Arrays.asList("coords", "coordinates", "location", "coordsLabel")) {
            if (row.containsKey(key) && row.get(key) != null) {
                String coordStr = row.get(key);

                if (coordStr.contains("Point")) {
                    try {
                        int start = coordStr.indexOf("(") + 1;
                        int end = coordStr.indexOf(")");

                        if (start > 0 && end > start) {
                            String content = coordStr.substring(start, end);
                            String[] parts = content.trim().split("\\s+");
                            if (parts.length >= 2) {
                                Map<String, Double> coords = new HashMap<>();
                                coords.put("lng", Double.parseDouble(parts[0]));
                                coords.put("lat", Double.parseDouble(parts[1]));
                                return coords;
                            }
                        }
                    } catch (Exception e) {
                        // Se captura la excepción pero se continua
                    }
                }
            }
        }

        if (row.containsKey("lat") && row.containsKey("lng")) {
            try {
                Map<String, Double> coords = new HashMap<>();
                coords.put("lat", Double.parseDouble(row.get("lat")));
                coords.put("lng", Double.parseDouble(row.get("lng")));
                return coords;
            } catch (NumberFormatException e) {
                // Se captura la excepción pero se continua
            }
        }

        return null;
    }


    /**
     * Metodo auxiliar para extraer label a partir de la uri
     * @param uri the uri
     * @return the label
     */
    private String extractLabelFromUri(String uri) {
        if (uri == null)
            return "";

        String[] parts = uri.split("[/#]");
        String label = parts[parts.length - 1];

        label = label.replaceAll("^(Q|P)\\d+$", label);

        return label;
    }

    /**
     * Metodo auxiliar para limpiar cadenas
     * @param value the string to clean
     * @return the value
     */
    private String cleanValue(String value) {
        if (value == null)
            return "";

        value = value.replaceAll("@[a-z]{2}$", "");

        value = value.replaceAll("\\^\\^.*$", "");

        return value.trim();
    }
}
