/**
 * Map View Component using Leaflet.js
 * Renders geographical coordinates on an interactive map
 */

const MapView = {
    map: null,
    markers: [],
    markersLayer: null,
    defaultCenter: [40.416775, -3.703790], // Madrid
    defaultZoom: 2,

    /**
     * Initialize map view
     */
    init() {
        // Map initialization is done on first render or when container is visible
        // because Leaflet needs the container to have dimensions
    },

    /**
     * Render map view with data
     * @param {Array} mapData - List of items with coordinates
     */
    render(mapData) {
        const container = document.getElementById('map-canvas');

        if (!mapData || mapData.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <h3>No hay coordenadas disponibles</h3>
                    <p>Los resultados actuales no contienen información geográfica para mostrar en el mapa.</p>
                </div>
            `;
            return;
        }

        // Initialize map if not exists
        if (!this.map) {
            this.map = L.map('map-canvas').setView(this.defaultCenter, this.defaultZoom);

            // Add OpenStreetMap tile layer
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(this.map);

            this.markersLayer = L.layerGroup().addTo(this.map);
        } else {
            // Clear existing markers
            this.markersLayer.clearLayers();
            // Force map to recalculate size (in case it was hidden)
            this.map.invalidateSize();
        }

        // Add markers
        const bounds = L.latLngBounds();
        let validMarkers = 0;

        mapData.forEach(item => {
            if (item.lat && item.lng) {
                const marker = L.marker([item.lat, item.lng]);

                // Create popup content
                const popupContent = `
                    <div class="map-popup">
                        <h3 class="map-popup-title">${item.title}</h3>
                        <p class="map-popup-description">${item.description || 'Sin descripción'}</p>
                        ${item.type ? `<span class="badge text-xs bg-gray-200 px-2 py-1 rounded mt-2 inline-block">${item.type}</span>` : ''}
                    </div>
                `;

                marker.bindPopup(popupContent);
                this.markersLayer.addLayer(marker);
                bounds.extend([item.lat, item.lng]);
                validMarkers++;
            }
        });

        // Fit map to bounds if markers exist
        if (validMarkers > 0) {
            this.map.fitBounds(bounds, { padding: [50, 50] });
        }
    }
};
