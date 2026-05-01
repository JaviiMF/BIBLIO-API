/**
 * Card View Implementation
 * Displays search results as visually appealing cards with images and metadata
 */

const CardView = {
    container: null,
    data: null,

    init() {
        this.container = document.getElementById('cards-view');
    },

    render(cardData) {
        if (!this.container) this.init();

        this.data = cardData;

        if (!cardData || cardData.length === 0) {
            this.renderEmptyState();
            return;
        }

        const grid = document.createElement('div');
        grid.className = 'cards-grid';

        cardData.forEach((card, index) => {
            const cardElement = this.createCard(card, index);
            grid.appendChild(cardElement);
        });

        this.container.innerHTML = '';
        this.container.appendChild(grid);
    },

    createCard(card, index) {
        const cardEl = document.createElement('div');
        cardEl.className = 'result-card';
        cardEl.setAttribute('data-index', index);

        // Image container
        const imageContainer = document.createElement('div');
        imageContainer.className = 'card-image-container';

        if (card.image) {
            const img = document.createElement('img');
            img.className = 'card-image';
            img.src = card.image;
            img.alt = card.title || 'Image';
            img.onerror = () => {
                imageContainer.classList.add('no-image');
                imageContainer.innerHTML = this.getDefaultIcon();
            };
            imageContainer.appendChild(img);
        } else {
            imageContainer.classList.add('no-image');
            imageContainer.innerHTML = this.getDefaultIcon();
        }

        // Type badge
        if (card.type && card.type !== 'unknown') {
            const badge = document.createElement('div');
            badge.className = 'card-type-badge';
            badge.textContent = this.formatType(card.type);
            imageContainer.appendChild(badge);
        }

        // Content
        const content = document.createElement('div');
        content.className = 'card-content';

        // Title
        const title = document.createElement('h3');
        title.className = 'card-title';
        title.textContent = card.title || 'Sin título';
        content.appendChild(title);

        // Description
        if (card.description) {
            const description = document.createElement('p');
            description.className = 'card-description';
            description.textContent = card.description;
            content.appendChild(description);
        }

        // Metadata tags
        if (card.metadata && Object.keys(card.metadata).length > 0) {
            const metadataContainer = document.createElement('div');
            metadataContainer.className = 'card-metadata';

            Object.entries(card.metadata).forEach(([key, value]) => {
                if (value && value.length < 50) { // Only show short metadata
                    const tag = document.createElement('span');
                    tag.className = 'metadata-tag';
                    tag.textContent = `${this.formatKey(key)}: ${value}`;
                    metadataContainer.appendChild(tag);
                }
            });

            content.appendChild(metadataContainer);
        }

        cardEl.appendChild(imageContainer);
        cardEl.appendChild(content);

        // Click handler
        cardEl.addEventListener('click', () => this.handleCardClick(card));

        return cardEl;
    },

    handleCardClick(card) {
        if (card.url) {
            window.open(card.url, '_blank');
        } else {
            // Show detail modal (could be implemented later)
            console.log('Card clicked:', card);
        }
    },

    formatType(type) {
        // Extract readable type from URI or clean up type string
        const typeMap = {
            'author': 'Autor',
            'work': 'Obra',
            'book': 'Libro',
            'topic': 'Tema',
            'place': 'Lugar',
            'person': 'Persona',
            'organization': 'Organización'
        };

        const lowerType = type.toLowerCase();
        for (const [key, value] of Object.entries(typeMap)) {
            if (lowerType.includes(key)) {
                return value;
            }
        }

        return type.split('/').pop().split('#').pop();
    },

    formatKey(key) {
        // Convert camelCase or snake_case to readable format
        return key
            .replace(/([A-Z])/g, ' $1')
            .replace(/_/g, ' ')
            .replace(/^./, str => str.toUpperCase())
            .trim();
    },

    getDefaultIcon() {
        return `
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
        `;
    },

    renderEmptyState() {
        this.container.innerHTML = `
            <div class="empty-state">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                </svg>
                <h3>No hay resultados</h3>
                <p>Intenta con una búsqueda diferente</p>
            </div>
        `;
    }
};
