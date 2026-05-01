/**
 * Graph View Implementation
 * Displays search results as an interactive network graph using vis.js
 */

const GraphView = {
    container: null,
    network: null,
    data: null,

    init() {
        this.container = document.getElementById('graph-canvas');
    },

    render(graphData) {
        if (!this.container) this.init();

        this.data = graphData;

        if (!graphData || !graphData.nodes || graphData.nodes.length === 0) {
            this.renderEmptyState();
            return;
        }

        // Prepare data for vis.js
        const nodes = new vis.DataSet(graphData.nodes.map(node => ({
            id: node.id,
            label: this.truncateLabel(node.label || node.id),
            title: node.title || node.label, // Tooltip
            group: this.normalizeGroup(node.group),
            shape: 'dot',
            size: 20,
            font: {
                size: 14,
                color: '#374151'
            }
        })));

        const edges = new vis.DataSet((graphData.edges || []).map(edge => ({
            id: edge.id,
            from: edge.from,
            to: edge.to,
            label: this.truncateLabel(edge.label || ''),
            arrows: 'to',
            font: {
                size: 10,
                color: '#6b7280',
                align: 'middle'
            },
            color: {
                color: '#d1d5db',
                highlight: '#667eea'
            }
        })));

        const data = { nodes, edges };

        // Network options
        const options = {
            nodes: {
                borderWidth: 2,
                borderWidthSelected: 4,
                color: {
                    border: '#667eea',
                    background: '#ffffff',
                    highlight: {
                        border: '#667eea',
                        background: '#eef2ff'
                    }
                }
            },
            edges: {
                width: 2,
                smooth: {
                    type: 'continuous',
                    roundness: 0.5
                }
            },
            groups: {
                author: { color: { background: '#dbeafe', border: '#3b82f6' } },
                work: { color: { background: '#d1fae5', border: '#10b981' } },
                topic: { color: { background: '#fef3c7', border: '#f59e0b' } },
                place: { color: { background: '#fee2e2', border: '#ef4444' } },
                person: { color: { background: '#dbeafe', border: '#3b82f6' } },
                organization: { color: { background: '#ede9fe', border: '#8b5cf6' } },
                event: { color: { background: '#ffedd5', border: '#f97316' } },
                unknown: { color: { background: '#f3f4f6', border: '#6b7280' } }
            },
            physics: {
                enabled: true,
                barnesHut: {
                    gravitationalConstant: -2000,
                    centralGravity: 0.3,
                    springLength: 150,
                    springConstant: 0.04,
                    damping: 0.09
                },
                stabilization: {
                    iterations: 200
                }
            },
            interaction: {
                hover: true,
                tooltipDelay: 100,
                navigationButtons: true,
                keyboard: true
            }
        };

        // Create network
        this.network = new vis.Network(this.container, data, options);

        // Event handlers
        this.network.on('click', (params) => {
            if (params.nodes.length > 0) {
                const nodeId = params.nodes[0];
                this.handleNodeClick(nodeId);
            }
        });

        this.network.on('stabilizationIterationsDone', () => {
            this.network.setOptions({ physics: false });
        });

        // Render legend
        this.renderLegend();
    },

    handleNodeClick(nodeId) {
        const node = this.data.nodes.find(n => n.id === nodeId);
        if (node && node.id.startsWith('http')) {
            window.open(node.id, '_blank');
        }
    },

    truncateLabel(label, maxLength = 30) {
        if (!label) return '';
        return label.length > maxLength ? label.substring(0, maxLength) + '...' : label;
    },

    normalizeGroup(group) {
        if (!group) return 'unknown';

        // 1. Si el grupo es una URL, nos quedamos con la última parte
        let g = group.toLowerCase().trim();
        if (g.includes('/') || g.includes('#')) {
            g = g.split(/[/#]/).pop();
        }

        // --- WORK / BOOK / ART ---
        if (
            /\b(work|book|novel|edition|manuscript|publication|instance|item|volume|text|manifestation|document|monograph|atlas|map|score|creativework|movie|film|artwork|painting|sculpture|album|song|thesis|dissertation|article|report|statute|law|poem|play|series|show|game|videogame|software|newspaper|magazine|periodical|resource|expression|holding|copy)\b/.test(g) ||
            /\b(obra|libro|novela|edición|manuscrito|publicación|ejemplar|volumen|texto|manifestación|documento|monografía|atlas|mapa|partitura|película|filme|cuadro|pintura|escultura|álbum|canción|obra de arte|tesis|disertación|artículo|informe|estatuto|ley|poema|teatro|serie|show|juego|videojuego|software|periódico|diario|revista|publicación periódica|recurso|expresion|expresión|fondo)\b/.test(g) ||
            /\b(œuvre|livre|roman|édition|manuscrit|publication|exemplaire|volume|texte|manifestation|document|monographie|carte|partition|film|peinture|sculpture|album|chanson|œuvre d'art|thèse|dissertation|article|rapport|statut|loi|poème|pièce|série|jeu|logiciel|journal|magazine|périodique)\b/.test(g) ||
            /\b(werk|buch|roman|ausgabe|manuskript|publikation|band|text|dokument|film|gemälde|skulptur|album|lied|kunstwerk|these|dissertation|artikel|bericht|gesetz|gedicht|stück|serie|spiel|software|zeitung|zeitschrift)\b/.test(g) ||
            /\b(opera|libro|romanzo|edizione|manoscritto|pubblicazione|volume|testo|documento|film|pellicola|dipinto|pittura|scultura|album|canzone|opera d'arte|tesi|dissertazione|articolo|rapporto|statuto|legge|poema|gioco|software|giornale|rivista|periodico)\b/.test(g) ||
            /\b(livro|romance|edição|manuscrito|publicação|volume|texto|documento|filme|pintura|escultura|álbum|canção|obra de arte|tese|dissertação|artigo|relatório|estatuto|lei|poema|peça|série|jogo|software|jornal|revista|periódico)\b/.test(g) ||
            /q571|q7725634|q4746138|q333|q234460|q1924249|q11424|q386724|q29014596|q1344|q11032|q7397|c1001|c1002|c1003|c1004|c1007|c1008/.test(g) // C1001: Obra, C1004: Ejemplar, C1008: Marca prop.
        ) return 'work';

        // --- PLACE / LOCATION ---
        if (
           /\b(place|district|location|city|country|state|territory|address|site|region|province|continent|municipality|village|town|river|mountain|capital|park|forest|island|lake|sea|ocean|street|square|building|monument|station|airport|church|cathedral|castle|palace|museum|theater|stadium)\b/.test(g) ||
           /\b(lugar|distrito|parroquia|ubicación|ciudad|país|estado|territorio|dirección|sitio|región|provincia|continente|municipio|pueblo|sede|río|montaña|capital|parque|bosque|isla|lago|mar|océano|calle|plaza|edificio|monumento|estación|aeropuerto|iglesia|catedral|castillo|palacio|museo|teatro|estadio)\b/.test(g) ||
           /\b(lieu|endroit|ville|pays|état|territoire|adresse|site|région|province|continent|commune|village|rivière|fleuve|montagne|capitale|parc|forêt|île|lac|mer|océan|rue|place|bâtiment|monument|gare|aéroport|église|cathédrale|château|palais|musée|théâtre|stade)\b/.test(g) ||
           /\b(ort|stadt|land|staat|territorium|region|provinz|kontinent|gemeinde|dorf|fluss|berg|hauptstadt|park|wald|insel|see|meer|ozean|straße|platz|gebäude|denkmal|station|bahnhof|flughafen|kirche|dom|schloss|palast|museum|theater|stadion)\b/.test(g) ||
           /\b(luogo|città|paese|stato|territorio|sito|regione|provincia|continente|comune|villaggio|fiume|montagna|capitale|parco|foresta|isola|lago|mare|oceano|strada|piazza|edificio|monumento|stazione|aeroporto|chiesa|cattedrale|castello|palazzo|museo|teatro|stadio)\b/.test(g) ||
           /\b(lugar|local|cidade|país|estado|território|sítio|região|província|continente|município|vila|rio|montanha|capital|parque|floresta|ilha|lago|mar|oceano|rua|praça|edifício|monumento|estação|aeroporto|igreja|catedral|castelo|palácio|museu|teatro|estádio)\b/.test(g) ||
           /q2221906|q618123|q8274|q6256|q515|q4022|q22676|q165|q23413|q1339|q839954/.test(g) // Q6256: Country, Q515: City
        ) return 'place';

        // --- ORGANIZATION ---
        if (
           /\b(organization|publisher|library|university|institution|corp|company|foundation|association|group|band|school|college|institute|agency|department|ministry|party|team|club|network|broadcaster|corporate body)\b/.test(g) ||
           /\b(organización|editorial|biblioteca|universidad|institución|empresa|fundación|asociación|grupo|colectividad|entidad|banda|escuela|colegio|instituto|agencia|departamento|ministerio|partido|equipo|club|red|emisora|cadena|corporativa)\b/.test(g) ||
           /\b(organisation|éditeur|bibliothèque|université|institution|entreprise|société|fondation|association|groupe|collectivité|école|collège|institut|agence|département|ministère|parti|équipe|réseau)\b/.test(g) ||
           /\b(organisation|verlag|bibliothek|universität|institution|unternehmen|firma|stiftung|verein|gruppe|band|schule|hochschule|institut|agentur|abteilung|ministerium|partei|team|club|netzwerk|sender)\b/.test(g) ||
           /\b(organizzazione|editore|biblioteca|università|istituzione|azienda|impresa|fondazione|associazione|gruppo|scuola|collegio|istituto|agenzia|dipartimento|ministero|partito|squadra|rete|emittente)\b/.test(g) ||
           /\b(organização|editora|biblioteca|universidade|instituição|empresa|fundação|associação|grupo|banda|escola|colégio|instituto|agência|departamento|ministério|partido|time|clube|rede|emissora)\b/.test(g) ||
           /q43229|q7075|q3918|q151457|q4830453|q3914|q188094|q327333|c1006/.test(g) // C1006: Entidad Corporativa
        ) return 'organization';

        // --- TOPIC / CONCEPT ---
        if (
           /\b(topic|subject|concept|genre|theme|term|category|class|form|heading|discipline|field|shorthand|taxon|species|disease|compound|theory|method|system|problem|phenomenon|process|movement|style|language)\b/.test(g) ||
           /\b(tema|materia|asteroide|concepto|género|término|categoría|clase|encabezamiento|disciplina|campo|taxón|especie|enfermedad|compuesto|teoría|método|sistema|problema|fenómeno|proceso|movimiento|estilo|lenguaje|idioma)\b/.test(g) ||
           /\b(sujet|concept|genre|thème|terme|catégorie|classe|matière|vedette|discipline|domaine|taxon|espèce|maladie|composé|théorie|méthode|système|problème|phénomène|processus|mouvement|style|langue|langage)\b/.test(g) ||
           /\b(thema|konzept|genre|begriff|kategorie|klasse|disziplin|feld|taxon|art|krankheit|verbindung|theorie|methode|system|problem|phänomen|prozess|bewegung|stil|sprache)\b/.test(g) ||
           /\b(argomento|soggetto|concetto|genere|tema|termine|categoria|classe|disciplina|campo|taxon|specie|malattia|composto|teoria|metodo|sistema|problema|fenomeno|processo|movimento|stile|lingua|linguaggio)\b/.test(g) ||
           /\b(tópico|assunto|conceito|gênero|tema|termo|categoria|classe|disciplina|campo|táxon|espécie|doença|composto|teoria|método|sistema|problema|fenômeno|processo|movimento|estilo|língua|linguagem)\b/.test(g) ||
           /q151885|q1292061|q11282|q42938|q16521|q12136|q34770|q9143/.test(g) // Q16521: Taxon
        ) return 'topic';

        // --- AUTHOR / PERSON ---
        // Eng, Spa, Fre, Deu, Ita, Por (Expanded)
        if (
           /\b(author|person|human|creator|writer|agent|performer|artist|contributor|illustrator|singer|composer|translator|conductor|director|man|woman|architect|politician|actor|actress|player|athlete|scientist|researcher|philosopher|monarch|king|queen|president|poet|painter|sculptor|photographer|journalist|historian|teacher|professor)\b/.test(g) ||
           /\b(autor|persona|humano|creador|escritor|agente|artista|colaborador|ilustrador|cantante|compositor|traductor|director|intérprete|hombre|mujer|ser humano|arquitecto|político|actor|actriz|jugador|atleta|científico|investigador|filósofo|monarca|rey|reina|presidente|poeta|pintor|escultor|fotógrafo|periodista|historiador|maestro|profesor)\b/.test(g) ||
           /\b(auteur|personne|humain|écrivain|créateur|artiste|contributeur|illustrateur|chanteur|compositeur|traducteur|interprète|homme|femme|architecte|politicien|acteur|actrice|joueur|athlète|scientifique|chercheur|philosophe|monarque|roi|reine|président|poète|peintre|sculpteur|photographe|journaliste|historien|enseignant|professeur)\b/.test(g) ||
           /\b(mensch|person|mann|frau|autor|künstler|komponist|sänger|schriftsteller|architekt|politiker|schauspieler|spieler|athlet|wissenschaftler|forscher|philosoph|monarch|könig|königin|präsident|dichter|maler|bildhauer|fotograf|journalist|historiker|lehrer|professor)\b/.test(g) ||
           /\b(umano|persona|uomo|donna|autore|artista|cantante|compositore|scrittore|architetto|politico|attore|attrice|giocatore|atleta|scienziato|ricercatore|filosofo|monarca|re|regina|presidente|poeta|pittore|scultore|fotografo|giornalista|storico|insegnante|professore)\b/.test(g) ||
           /\b(pessoa|humano|homem|mulher|autor|artista|cantor|compositor|escritor|arquiteto|político|ator|atriz|jogador|atleta|cientista|pesquisador|filósofo|monarca|rei|rainha|presidente|poeta|pintor|escultor|fotógrafo|jornalista|historiador|professor|mestre)\b/.test(g) ||
           /q5|q482980|q215627|q36180|q33999|q1930187|q2526255|q484876|q205375|c1005/.test(g) // Q5: Human, Q33999: Actor
        ) return 'author';

        // --- EVENT (Minor category, but useful) ---
        if (
           /\b(event|conference|exhibition|meeting|date|year|period|century|festival|workshop|concert|tournament|match|war|battle|election)\b/.test(g) ||
           /\b(evento|conferencia|exposición|reunión|fecha|año|periodo|siglo|festival|taller|concierto|torneo|partido|guerra|batalla|elección)\b/.test(g) ||
           /\b(événement|conférence|exposition|réunion|date|année|période|siècle|festival|atelier|concert|tournoi|match|guerre|bataille|élection)\b/.test(g) ||
           /\b(ereignis|konferenz|ausstellung|treffen|datum|jahr|zeitraum|jahrhundert|festival|konzert|turnier|spiel|krieg|schlacht|wahl)\b/.test(g) ||
           /\b(evento|conferenza|esposizione|riunione|data|anno|periodo|secolo|festival|concerto|torneo|partita|guerra|battaglia|elezione)\b/.test(g) ||
           /\b(evento|conferência|exposição|reunião|data|ano|período|século|festival|concerto|torneio|jogo|guerra|batalha|eleição)\b/.test(g) ||
           /q1190554|q1656682|q198|q178561/.test(g) // Q198: War
        ) return 'event';

        // Log unknown types to help debugging
        console.warn('Unknown group/type encountered:', group);
        return 'unknown';
    },

    renderLegend() {
        const legendContainer = document.createElement('div');
        legendContainer.className = 'graph-legend';
        legendContainer.innerHTML = `
            <h4>Leyenda de Nodos</h4>
            <div class="legend-items">
                <div class="legend-item">
                    <span class="legend-color" style="background: #dbeafe; border: 2px solid #3b82f6;"></span>
                    <span>Autores / Personas</span>
                </div>
                <div class="legend-item">
                    <span class="legend-color" style="background: #d1fae5; border: 2px solid #10b981;"></span>
                    <span>Obras / Libros</span>
                </div>
                <div class="legend-item">
                    <span class="legend-color" style="background: #fef3c7; border: 2px solid #f59e0b;"></span>
                    <span>Temas / Conceptos</span>
                </div>
                <div class="legend-item">
                    <span class="legend-color" style="background: #fee2e2; border: 2px solid #ef4444;"></span>
                    <span>Lugares / Geografía</span>
                </div>
                <div class="legend-item">
                    <span class="legend-color" style="background: #ede9fe; border: 2px solid #8b5cf6;"></span>
                    <span>Organizaciones</span>
                </div>
                <div class="legend-item">
                    <span class="legend-color" style="background: #ffedd5; border: 2px solid #f97316;"></span>
                    <span>Eventos / Tiempo</span>
                </div>
                <div class="legend-item">
                    <span class="legend-color" style="background: #f3f4f6; border: 2px solid #6b7280;"></span>
                    <span>Otros / Desconocido</span>
                </div>
            </div>
        `;

        const parent = this.container.parentElement;
        const existingLegend = parent.querySelector('.graph-legend');
        if (existingLegend) existingLegend.remove();
        parent.appendChild(legendContainer);
    },

    renderEmptyState() {
        this.container.innerHTML = `
            <div class="empty-state">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                </svg>
                <h3>No hay datos para visualizar</h3>
                <p>No se encontraron relaciones en los resultados</p>
            </div>
        `;
    },

    destroy() {
        if (this.network) {
            this.network.destroy();
            this.network = null;
        }
    }
};
