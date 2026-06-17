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

        if (card.type && card.type !== 'unknown') {
            const badge = document.createElement('div');
            badge.className = 'card-type-badge';
            badge.textContent = this.formatType(card.type);
            imageContainer.appendChild(badge);
        }

        const content = document.createElement('div');
        content.className = 'card-content';

        const title = document.createElement('h3');
        title.className = 'card-title';
        title.textContent = card.title || 'Sin título';
        content.appendChild(title);

        if (card.description) {
            const description = document.createElement('p');
            description.className = 'card-description';
            description.textContent = card.description;
            content.appendChild(description);
        }

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

        cardEl.addEventListener('click', () => this.handleCardClick(card));

        return cardEl;
    },

    handleCardClick(card) {
        if (card.url) {
            window.open(card.url, '_blank');
        } else {
            console.log('Card clicked:', card);
        }
    },

    formatType(type) {
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
        return key
            .replace(/([A-Z])/g, ' $1')
            .replace(/_/g, ' ')
            .replace(/^./, str => str.toUpperCase())
            .trim();
    },

    getDefaultIcon() {
        return `
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
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
