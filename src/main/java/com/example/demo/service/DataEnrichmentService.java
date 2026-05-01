package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataEnrichmentService {

    /**
     * Enriches raw SPARQL results for multiple view types
     */
    public Map<String, Object> enrichResults(List<Map<String, String>> results) {
        Map<String, Object> enrichedData = new HashMap<>();

        enrichedData.put("rawData", results);
        enrichedData.put("cardData", enrichForCardView(results));
        enrichedData.put("graphData", enrichForGraphView(results));
        // enrichedData.put("timelineData", enrichForTimelineView(results));
        enrichedData.put("mapData", enrichForMapView(results));
        enrichedData.put("availableViews", detectAvailableViews(results));

        return enrichedData;
    }

    /**
     * Detect which views are viable based on available data
     */
    public List<String> detectAvailableViews(List<Map<String, String>> results) {
        List<String> availableViews = new ArrayList<>();

        // Cards and Table are always available
        availableViews.add("cards");
        availableViews.add("table");

        if (results.isEmpty()) {
            return availableViews;
        }

        // Check for graph data (relationships)
        boolean hasRelationships = results.stream()
                .anyMatch(row -> row.keySet().stream()
                        .anyMatch(key -> key.contains("related") || key.contains("type") || key.contains("predicate")));
        if (hasRelationships || results.size() > 1) {
            availableViews.add("graph");
        }

        // Check for timeline data (dates)
        boolean hasDates = results.stream()
                .anyMatch(row -> row.keySet().stream()
                        .anyMatch(key -> key.toLowerCase().contains("date") ||
                                key.toLowerCase().contains("year") ||
                                key.toLowerCase().contains("inception") ||
                                key.toLowerCase().contains("publication")));
        if (hasDates) {
            availableViews.add("timeline");
        }

        // Check for map data (coordinates)
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
     * Enrich data for card view
     */
    public List<Map<String, Object>> enrichForCardView(List<Map<String, String>> results) {
        return results.stream().map(row -> {
            Map<String, Object> card = new HashMap<>();

            // Extract main fields
            String title = extractTitle(row);
            String description = extractDescription(row);
            String image = extractImage(row);
            String type = extractType(row);
            String url = extractUrl(row);

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
     * Enrich data for graph view
     */
    public Map<String, Object> enrichForGraphView(List<Map<String, String>> results) {
        Map<String, Object> graphData = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> addedNodes = new HashSet<>();

        int edgeId = 0;

        for (Map<String, String> row : results) {
            // Create main node
            String nodeId = extractNodeId(row);
            if (!addedNodes.contains(nodeId)) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", nodeId);
                node.put("label", extractTitle(row));
                node.put("title", extractDescription(row)); // Tooltip
                node.put("group", extractType(row));
                nodes.add(node);
                addedNodes.add(nodeId);
            }

            // Create edges if relationships exist
            for (Map.Entry<String, String> entry : row.entrySet()) {
                String key = entry.getKey().toLowerCase();
                if (key.contains("related") || key.contains("type") || key.contains("predicate")) {
                    String relatedId = entry.getValue();
                    if (relatedId != null && !relatedId.isEmpty() && !relatedId.equals(nodeId)) {
                        // Add related node if not exists
                        if (!addedNodes.contains(relatedId)) {
                            Map<String, Object> relatedNode = new HashMap<>();
                            relatedNode.put("id", relatedId);
                            relatedNode.put("label", extractLabelFromUri(relatedId));
                            relatedNode.put("group", key);
                            nodes.add(relatedNode);
                            addedNodes.add(relatedId);
                        }

                        // Add edge
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

    /**
     * Enrich data for timeline view
     */
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
     * Enrich data for map view
     */
    /**
     * Enrich data for map view
     */
    public List<Map<String, Object>> enrichForMapView(List<Map<String, String>> results) {
        List<Map<String, Object>> markers = new ArrayList<>();

        for (Map<String, String> row : results) {
            Map<String, Double> coords = extractCoordinates(row);

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

    // Helper methods for data extraction

    private String extractTitle(Map<String, String> row) {
        // Try common label fields
        for (String key : Arrays.asList("label", "name", "title", "itemLabel", "sLabel")) {
            if (row.containsKey(key) && row.get(key) != null) {
                return cleanValue(row.get(key));
            }
        }
        // Fallback to first non-URI value
        return row.values().stream()
                .filter(v -> v != null && !v.startsWith("http"))
                .findFirst()
                .map(this::cleanValue)
                .orElse("Sin título");
    }

    private String extractDescription(Map<String, String> row) {
        for (String key : Arrays.asList("description", "abstract", "comment", "descriptionLabel")) {
            if (row.containsKey(key) && row.get(key) != null) {
                return cleanValue(row.get(key));
            }
        }
        return "";
    }

    private String extractImage(Map<String, String> row) {
        for (String key : Arrays.asList("image", "img", "picture", "thumbnail", "imageLabel")) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key);
            }
        }
        return null;
    }

    private String extractType(Map<String, String> row) {
        for (String key : Arrays.asList("type", "typeLabel", "class", "category")) {
            if (row.containsKey(key) && row.get(key) != null) {
                return extractLabelFromUri(row.get(key));
            }
        }
        return "unknown";
    }

    private String extractUrl(Map<String, String> row) {
        // Try to find the main entity URI
        for (String key : Arrays.asList("item", "s", "subject", "source", "uri")) {
            if (row.containsKey(key) && row.get(key) != null && row.get(key).startsWith("http")) {
                return row.get(key);
            }
        }
        return null;
    }

    private String extractNodeId(Map<String, String> row) {
        String url = extractUrl(row);
        return url != null ? url : "node_" + row.hashCode();
    }

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

    private String extractDate(Map<String, String> row) {
        for (String key : Arrays.asList("date", "publicationDate", "inception", "year", "dateLabel")) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key);
            }
        }
        return null;
    }

    private String parseDate(String dateStr) {
        // Simple date parsing - extract year if possible
        if (dateStr.matches("\\d{4}.*")) {
            return dateStr.substring(0, 4);
        }
        return dateStr;
    }

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
                        // Error parsing this specific field, continue to next
                    }
                }
            }
        }

        // Try separate lat/lng fields (el resto de tu lógica se mantiene igual)
        if (row.containsKey("lat") && row.containsKey("lng")) {
            try {
                Map<String, Double> coords = new HashMap<>();
                coords.put("lat", Double.parseDouble(row.get("lat")));
                coords.put("lng", Double.parseDouble(row.get("lng")));
                return coords;
            } catch (NumberFormatException e) { }
        }

        return null;
    }

    private String extractLabelFromUri(String uri) {
        if (uri == null)
            return "";

        // Extract last part of URI
        String[] parts = uri.split("[/#]");
        String label = parts[parts.length - 1];

        // Clean up common prefixes
        label = label.replaceAll("^(Q|P)\\d+$", label); // Keep Wikidata IDs as-is

        return label;
    }

    private String cleanValue(String value) {
        if (value == null)
            return "";

        // Remove language tags like @en, @es
        value = value.replaceAll("@[a-z]{2}$", "");

        // Remove datatype annotations
        value = value.replaceAll("\\^\\^.*$", "");

        return value.trim();
    }
}
