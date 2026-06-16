package com.example.demo.service;

import com.example.demo.model.Busqueda;
import com.example.demo.model.Usuario;
import com.example.demo.repository.BusquedaRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BusquedaService {

    @Autowired
    private BusquedaRepository busquedaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Metodo para grabar la búsqueda en base de datos
     * @param topic the topic
     * @param repository the destiny repository
     * @param queryToExecute the query to execute
     * @param results the results retrieved
     * @param endTime the time elapsed
     * @param startTime the stating time
     */
    public void saveBusqueda(String topic, String repository, String queryToExecute, List<Map<String, String>> results, long endTime, long startTime) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username);

        if (usuario != null) {
            Busqueda busqueda = new Busqueda();
            busqueda.setUsuario(usuario);
            busqueda.setTerminoBusqueda(topic != null ? topic : "Custom Query");
            busqueda.setQuerySparql(queryToExecute);
            busqueda.setEndpointsConsultados(repository);
            busqueda.setTotalResultados(results.size());
            busqueda.setTiempoEjecucionMs(endTime - startTime);
            busquedaRepository.save(busqueda);
        }
    }
}
