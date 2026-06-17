const MapView = {
    map: null,
    markers: [],
    markersLayer: null,
    defaultCenter: [40.416775, -3.703790], // Madrid
    defaultZoom: 2,

    init() {
    },

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

        if (!this.map) {
            this.map = L.map('map-canvas').setView(this.defaultCenter, this.defaultZoom);

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(this.map);

            this.markersLayer = L.layerGroup().addTo(this.map);
        } else {
            this.markersLayer.clearLayers();
            this.map.invalidateSize();
        }

        const bounds = L.latLngBounds();
        let validMarkers = 0;

        mapData.forEach(item => {
            if (item.lat && item.lng) {
                const marker = L.marker([item.lat, item.lng]);

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

        if (validMarkers > 0) {
            this.map.fitBounds(bounds, { padding: [50, 50] });
        }
    }
};
