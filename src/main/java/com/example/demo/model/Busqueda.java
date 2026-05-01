package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "busquedas")
public class Busqueda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "termino_busqueda", nullable = false)
    private String terminoBusqueda;

    @Column(name = "query_sparql", nullable = false, columnDefinition = "TEXT")
    private String querySparql;

    @Column(name = "endpoints_consultados", columnDefinition = "TEXT")
    private String endpointsConsultados;

    @Column(name = "total_resultados")
    private Integer totalResultados = 0;

    @Column(name = "tiempo_ejecucion_ms")
    private Long tiempoEjecucionMs;

    @Column(name = "fecha_busqueda", insertable = false, updatable = false)
    private LocalDateTime fechaBusqueda;

    // Constructors
    public Busqueda() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getTerminoBusqueda() {
        return terminoBusqueda;
    }

    public void setTerminoBusqueda(String terminoBusqueda) {
        this.terminoBusqueda = terminoBusqueda;
    }

    public String getQuerySparql() {
        return querySparql;
    }

    public void setQuerySparql(String querySparql) {
        this.querySparql = querySparql;
    }

    public String getEndpointsConsultados() {
        return endpointsConsultados;
    }

    public void setEndpointsConsultados(String endpointsConsultados) {
        this.endpointsConsultados = endpointsConsultados;
    }

    public Integer getTotalResultados() {
        return totalResultados;
    }

    public void setTotalResultados(Integer totalResultados) {
        this.totalResultados = totalResultados;
    }

    public Long getTiempoEjecucionMs() {
        return tiempoEjecucionMs;
    }

    public void setTiempoEjecucionMs(Long tiempoEjecucionMs) {
        this.tiempoEjecucionMs = tiempoEjecucionMs;
    }

    public LocalDateTime getFechaBusqueda() {
        return fechaBusqueda;
    }

    public void setFechaBusqueda(LocalDateTime fechaBusqueda) {
        this.fechaBusqueda = fechaBusqueda;
    }
}
