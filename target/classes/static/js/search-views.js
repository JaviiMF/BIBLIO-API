/**
 * Main Search Views Controller
 * Manages view switching and initialization
 */

const SearchViews = {
    currentView: 'cards',
    enrichedData: null,
    availableViews: [],

    init(enrichedDataJson) {
        try {
            this.enrichedData = typeof enrichedDataJson === 'string'
                ? JSON.parse(enrichedDataJson)
                : enrichedDataJson;

            this.availableViews = this.enrichedData.availableViews || ['cards', 'table'];

            this.setupViewSwitcher();
            this.initializeViews();
            this.switchView(this.currentView);
        } catch (error) {
            console.error('Error initializing SearchViews:', error);
        }
    },

    setupViewSwitcher() {
        const buttons = document.querySelectorAll('.view-switcher button');

        buttons.forEach(button => {
            const viewName = button.getAttribute('data-view');

            // Disable unavailable views
            if (!this.availableViews.includes(viewName)) {
                button.classList.add('disabled');
                button.disabled = true;
                button.title = 'Vista no disponible para estos resultados';
            } else {
                button.addEventListener('click', () => {
                    if (!button.classList.contains('disabled')) {
                        this.switchView(viewName);
                    }
                });
            }
        });
    },

    initializeViews() {
        // Initialize all view modules
        CardView.init();
        GraphView.init();
        MapView.init();
        TableView.init();
    },

    switchView(viewName) {
        if (!this.availableViews.includes(viewName)) {
            console.warn(`View ${viewName} is not available`);
            return;
        }

        this.currentView = viewName;

        // Update button states
        document.querySelectorAll('.view-switcher button').forEach(btn => {
            btn.classList.remove('active');
            if (btn.getAttribute('data-view') === viewName) {
                btn.classList.add('active');
            }
        });

        // Hide all view containers
        document.querySelectorAll('.view-container').forEach(container => {
            container.classList.remove('active');
        });

        // Show selected view container
        const activeContainer = document.getElementById(`${viewName}-view`);
        if (activeContainer) {
            activeContainer.classList.add('active');
        }

        // Render the view
        this.renderView(viewName);
    },

    renderView(viewName) {
        try {
            switch (viewName) {
                case 'graph':
                    GraphView.render(this.enrichedData.graphData);
                    break;
                case 'cards':
                    CardView.render(this.enrichedData.cardData);
                    break;
                case 'table':
                    TableView.render(this.enrichedData.rawData);
                    break;
                case 'timeline':
                    // TimelineView.render(this.enrichedData.timelineData);
                    this.renderPlaceholder('timeline', 'Línea de Tiempo');
                    break;
                case 'map':
                    // MapView.render(this.enrichedData.mapData);
                    // this.renderPlaceholder('map', 'Mapa');
                    MapView.render(this.enrichedData.mapData);
                    break;
                default:
                    console.warn(`Unknown view: ${viewName}`);
            }
        } catch (error) {
            console.error(`Error rendering ${viewName} view:`, error);
        }
    },

    renderPlaceholder(viewName, viewTitle) {
        const container = document.getElementById(`${viewName}-view`);
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <h3>${viewTitle} - Próximamente</h3>
                    <p>Esta vista estará disponible en una futura actualización</p>
                </div>
            `;
        }
    }
};

// Auto-initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const enrichedDataElement = document.getElementById('enriched-data');
    if (enrichedDataElement) {
        const enrichedData = enrichedDataElement.textContent;
        SearchViews.init(enrichedData);
    }
});
