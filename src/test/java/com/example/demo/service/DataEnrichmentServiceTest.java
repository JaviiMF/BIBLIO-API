package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataEnrichmentServiceTest {

    private DataEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new DataEnrichmentService();
    }

    @Test
    void detectAvailableViews_siempreIncluyeCardsYTable() {
        List<String> views = service.detectAvailableViews(Collections.emptyList());

        assertThat(views).contains("cards", "table");
    }

    @Test
    void detectAvailableViews_cuandoHayFechas_incluyeTimeline() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "Don Quijote");
        row.put("date", "1605");

        List<String> views = service.detectAvailableViews(List.of(row));

        assertThat(views).contains("timeline");
    }

    @Test
    void detectAvailableViews_cuandoHayCoordenadas_incluyeMap() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "Madrid");
        row.put("coords", "Point(-3.7 40.4)");

        List<String> views = service.detectAvailableViews(List.of(row));

        assertThat(views).contains("map");
    }

    @Test
    void detectAvailableViews_cuandoHayMasDeUnResultado_incluyeGraph() {
        Map<String, String> row1 = Map.of("label", "A");
        Map<String, String> row2 = Map.of("label", "B");

        List<String> views = service.detectAvailableViews(List.of(row1, row2));

        assertThat(views).contains("graph");
    }

    @Test
    void detectAvailableViews_conListaVacia_soloCardsYTable() {
        List<String> views = service.detectAvailableViews(Collections.emptyList());

        assertThat(views).containsExactlyInAnyOrder("cards", "table");
        assertThat(views).doesNotContain("timeline", "map", "graph");
    }

    @Test
    void enrichForCardView_mapeaCamposTituloYDescripcion() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "Cervantes");
        row.put("description", "Escritor español");

        List<Map<String, Object>> cards = service.enrichForCardView(List.of(row));

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).get("title")).isEqualTo("Cervantes");
        assertThat(cards.get(0).get("description")).isEqualTo("Escritor español");
    }

    @Test
    void enrichForCardView_cuandoNoHayTitulo_usaSinTitulo() {
        Map<String, String> row = new HashMap<>();
        row.put("item", "http://www.wikidata.org/entity/Q5593");

        List<Map<String, Object>> cards = service.enrichForCardView(List.of(row));

        assertThat(cards.get(0).get("title")).isEqualTo("Sin título");
    }

    @Test
    void enrichForCardView_extraeUrlDesdeItemField() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "Cervantes");
        row.put("item", "http://www.wikidata.org/entity/Q5593");

        List<Map<String, Object>> cards = service.enrichForCardView(List.of(row));

        assertThat(cards.get(0).get("url")).isEqualTo("http://www.wikidata.org/entity/Q5593");
    }

    @Test
    void enrichForCardView_limpiaTags_deIdioma() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "Miguel de Cervantes@es");

        List<Map<String, Object>> cards = service.enrichForCardView(List.of(row));

        assertThat(cards.get(0).get("title")).isEqualTo("Miguel de Cervantes");
    }

    @Test
    void enrichForCardView_conListaVacia_devuelveListaVacia() {
        List<Map<String, Object>> cards = service.enrichForCardView(Collections.emptyList());

        assertThat(cards).isEmpty();
    }

    @Test
    void enrichForGraphView_creaNodesPorCadaFila() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "Cervantes");
        row.put("item", "http://www.wikidata.org/entity/Q5593");

        Map<String, Object> graph = service.enrichForGraphView(List.of(row));

        List<?> nodes = (List<?>) graph.get("nodes");
        assertThat(nodes).hasSize(1);
    }

    @Test
    void enrichForGraphView_noCreaDuplicadosDeNodos() {
        Map<String, String> row1 = new HashMap<>();
        row1.put("label", "Cervantes");
        row1.put("item", "http://www.wikidata.org/entity/Q5593");

        Map<String, String> row2 = new HashMap<>();
        row2.put("label", "Cervantes");
        row2.put("item", "http://www.wikidata.org/entity/Q5593");

        Map<String, Object> graph = service.enrichForGraphView(List.of(row1, row2));

        List<?> nodes = (List<?>) graph.get("nodes");
        assertThat(nodes).hasSize(1); // No duplicados
    }

    @Test
    void enrichForGraphView_conListaVacia_devuelveNodosYEdgesVacios() {
        Map<String, Object> graph = service.enrichForGraphView(Collections.emptyList());

        assertThat((List<?>) graph.get("nodes")).isEmpty();
        assertThat((List<?>) graph.get("edges")).isEmpty();
    }

    @Test
    void enrichForMapView_extraeCoordenadaPointFormat() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "Madrid");
        row.put("coords", "Point(-3.7 40.4)");

        List<Map<String, Object>> markers = service.enrichForMapView(List.of(row));

        assertThat(markers).hasSize(1);
        assertThat(markers.get(0).get("lat")).isEqualTo(40.4);
        assertThat(markers.get(0).get("lng")).isEqualTo(-3.7);
        assertThat(markers.get(0).get("title")).isEqualTo("Madrid");
    }

    @Test
    void enrichForMapView_cuandoNoHayCoordenadas_devuelveListaVacia() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "Sin coordenadas");

        List<Map<String, Object>> markers = service.enrichForMapView(List.of(row));

        assertThat(markers).isEmpty();
    }

    @Test
    void enrichForMapView_extraeCoordsDesdeLatLng() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "París");
        row.put("lat", "48.8566");
        row.put("lng", "2.3522");

        List<Map<String, Object>> markers = service.enrichForMapView(List.of(row));

        assertThat(markers).hasSize(1);
        assertThat(markers.get(0).get("lat")).isEqualTo(48.8566);
        assertThat(markers.get(0).get("lng")).isEqualTo(2.3522);
    }

    @Test
    void enrichResults_devuelveTodosLosGruposDeDatos() {
        Map<String, String> row = new HashMap<>();
        row.put("label", "Cervantes");

        Map<String, Object> enriched = service.enrichResults(List.of(row));

        assertThat(enriched).containsKeys("rawData", "cardData", "graphData", "mapData", "availableViews");
    }

    @Test
    void enrichResults_rawDataContieneLosResultadosOriginales() {
        Map<String, String> row = Map.of("label", "Cervantes");
        List<Map<String, String>> input = List.of(row);

        Map<String, Object> enriched = service.enrichResults(input);

        assertThat(enriched.get("rawData")).isEqualTo(input);
    }
}
