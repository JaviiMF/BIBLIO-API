/**
 * Table View Implementation
 * Enhanced table with sorting, filtering, and export capabilities
 */

const TableView = {
    container: null,
    data: null,
    filteredData: null,

    init() {
        this.container = document.getElementById('table-view');
    },

    render(rawData) {
        if (!this.container) this.init();

        this.data = rawData;
        this.filteredData = rawData;

        if (!rawData || rawData.length === 0) {
            this.renderEmptyState();
            return;
        }

        const tableContainer = document.createElement('div');
        tableContainer.className = 'table-container';

        // Controls
        const controls = this.createControls();
        tableContainer.appendChild(controls);

        // Table
        const tableWrapper = document.createElement('div');
        tableWrapper.className = 'overflow-x-auto';

        const table = this.createTable(rawData);
        tableWrapper.appendChild(table);
        tableContainer.appendChild(tableWrapper);

        this.container.innerHTML = '';
        this.container.appendChild(tableContainer);
    },

    createControls() {
        const controls = document.createElement('div');
        controls.className = 'table-controls';

        // Search input
        const searchInput = document.createElement('input');
        searchInput.type = 'text';
        searchInput.className = 'table-search';
        searchInput.placeholder = 'Buscar en resultados...';
        searchInput.addEventListener('input', (e) => this.handleSearch(e.target.value));

        // Export button
        const exportBtn = document.createElement('button');
        exportBtn.className = 'table-export-btn';
        exportBtn.textContent = '📥 Exportar CSV';
        exportBtn.addEventListener('click', () => this.exportToCSV());

        controls.appendChild(searchInput);
        controls.appendChild(exportBtn);

        return controls;
    },

    createTable(data) {
        if (data.length === 0) return document.createElement('div');

        const table = document.createElement('table');
        table.className = 'min-w-full leading-normal';

        // Header
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');

        const columns = Object.keys(data[0]);
        columns.forEach(col => {
            const th = document.createElement('th');
            th.className = 'px-5 py-3 border-b-2 border-gray-200 bg-gray-100 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider cursor-pointer hover:bg-gray-200';
            th.textContent = col;
            th.addEventListener('click', () => this.handleSort(col));
            headerRow.appendChild(th);
        });

        thead.appendChild(headerRow);
        table.appendChild(thead);

        // Body
        const tbody = document.createElement('tbody');
        data.forEach(row => {
            const tr = document.createElement('tr');
            tr.className = 'hover:bg-gray-50';

            columns.forEach(col => {
                const td = document.createElement('td');
                td.className = 'px-5 py-5 border-b border-gray-200 bg-white text-sm';

                const value = row[col] || '';

                // Check if value is a URL
                if (value.startsWith('http')) {
                    const link = document.createElement('a');
                    link.href = value;
                    link.target = '_blank';
                    link.className = 'text-blue-600 hover:underline';
                    link.textContent = this.truncateUrl(value);
                    td.appendChild(link);
                } else {
                    const p = document.createElement('p');
                    p.className = 'text-gray-900 whitespace-no-wrap';
                    p.textContent = value;
                    td.appendChild(p);
                }

                tr.appendChild(td);
            });

            tbody.appendChild(tr);
        });

        table.appendChild(tbody);

        return table;
    },

    handleSearch(query) {
        if (!query) {
            this.filteredData = this.data;
        } else {
            const lowerQuery = query.toLowerCase();
            this.filteredData = this.data.filter(row => {
                return Object.values(row).some(value =>
                    String(value).toLowerCase().includes(lowerQuery)
                );
            });
        }

        // Re-render table
        const tableWrapper = this.container.querySelector('.overflow-x-auto');
        if (tableWrapper) {
            const newTable = this.createTable(this.filteredData);
            tableWrapper.innerHTML = '';
            tableWrapper.appendChild(newTable);
        }
    },

    handleSort(column) {
        const isAscending = this.container.dataset.sortColumn === column &&
            this.container.dataset.sortOrder === 'asc';

        this.filteredData.sort((a, b) => {
            const aVal = String(a[column] || '');
            const bVal = String(b[column] || '');

            if (isAscending) {
                return bVal.localeCompare(aVal);
            } else {
                return aVal.localeCompare(bVal);
            }
        });

        this.container.dataset.sortColumn = column;
        this.container.dataset.sortOrder = isAscending ? 'desc' : 'asc';

        // Re-render table
        const tableWrapper = this.container.querySelector('.overflow-x-auto');
        if (tableWrapper) {
            const newTable = this.createTable(this.filteredData);
            tableWrapper.innerHTML = '';
            tableWrapper.appendChild(newTable);
        }
    },

    exportToCSV() {
        if (!this.filteredData || this.filteredData.length === 0) return;

        const columns = Object.keys(this.filteredData[0]);

        // Create CSV content
        let csv = columns.join(',') + '\n';

        this.filteredData.forEach(row => {
            const values = columns.map(col => {
                const value = String(row[col] || '');
                // Escape quotes and wrap in quotes if contains comma
                return value.includes(',') || value.includes('"')
                    ? `"${value.replace(/"/g, '""')}"`
                    : value;
            });
            csv += values.join(',') + '\n';
        });

        // Download
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);

        link.setAttribute('href', url);
        link.setAttribute('download', `biblio-results-${Date.now()}.csv`);
        link.style.visibility = 'hidden';

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    },

    truncateUrl(url, maxLength = 50) {
        if (url.length <= maxLength) return url;
        return url.substring(0, maxLength) + '...';
    },

    renderEmptyState() {
        this.container.innerHTML = `
            <div class="empty-state">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <h3>No hay datos</h3>
                <p>Realiza una búsqueda para ver resultados</p>
            </div>
        `;
    }
};
