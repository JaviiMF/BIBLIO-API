# Documentación Técnica Completa: Proyecto BIBLIO-API

<p align="left">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven" />
</p>

---

## 1. Guía del Proyecto

### 1.1. Descripción del Proyecto

**BIBLIO-API** es una plataforma avanzada de búsqueda bibliográfica cimentada en los principios de la *Web Semántica* y *Linked Data*. El núcleo del sistema está diseñado para trascender las búsquedas tradicionales mediante la implementación de capacidades de búsqueda federada y comparativas de calidad. Técnicamente, estas funcionalidades se materializan a través de las interfaces `federated-search.html` y `quality-comparative-search.html`.

### 1.2. Requisitos Previos

El sistema requiere los siguientes componentes:

* **Java 21**: Necesario para dar soporte a la arquitectura de Spring Boot 3.x utilizada.
* **Maven**: Herramienta esencial para la gestión del ciclo de vida del proyecto y resolución de dependencias mediante el archivo `pom.xml`.
* **PostgreSQL**: Motor relacional para la persistencia. Es fundamental destacar que la gestión de datos se realiza a través de *Spring Data JPA*, lo que abstrae la interacción con el esquema para entidades críticas como `Usuario` y `Busqueda`.

### 1.3. Instalación y Configuración

Pasos a seguir:

1.  **Clonación**:
    ```bash
    git clone <url-del-repositorio>
    ```
2.  **Configuración de Base de Datos**: Cree una base de datos en su instancia de PostgreSQL. Por convención técnica, se recomienda el nombre `biblio_api` para mantener la coherencia con el nombre del proyecto.
3.  **Archivo de Configuración**: Configure el entorno en `src/main/resources/application.properties` ajustando las propiedades de conexión.

### 1.4. Ejecución y Pruebas

Para levantar el proyecto en local, ejecute:

```bash
mvn spring-boot:run
```

La aplicación se inicia desde la clase `DemoApplication.java`. Como estándar de calidad, se incluye `DemoApplicationTests.java`. Las pruebas pueden lanzarse mediante el comando:

```bash
mvn test
```

---

## 2. Análisis de la Arquitectura del Frontend

### 2.1. Motor de Plantillas y Modularidad

El sistema emplea **Thymeleaf** para el renderizado. Se ha priorizado la modularidad mediante el uso de fragmentos en `templates/fragments/`. Archivos como `nav.html` y `navLogin.html` permiten un desacoplamiento entre la lógica de navegación y el contenido principal. Esta estrategia no solo reduce la redundancia de código, sino que facilita el mantenimiento de la interfaz global sin afectar individualmente a cada vista.

### 2.2. Visualización y Lógica en Cliente (JavaScript)

La visualización de datos se organiza en módulos específicos dentro de `static/js/views/`:

| Archivo | Funcionalidad de Visualización                                            |
| :--- |:--------------------------------------------------------------------------|
| `graph-view.js` | Generación de grafos de conocimiento para relaciones entre entidades RDF. |
| `map-view.js` | Representación de datos geoespaciales y geolocalización bibliográfica.    |
| `table-view.js` | Formateo de resultados en estructuras tabulares.                          |
| `card-view.js` | Visualización atómica de registros mediante tarjetas informativas.        |

El componente `search-views.js` actúa como el coordinador central en el cliente, no solo gestiona el salto entre vistas, sino que orquesta la lógica de consumo de los endpoints expuestos por el backend.

### 2.3. Interacción con Servicios Backend

El flujo de datos se establece mediante peticiones hacia los controladores del lado del servidor. `SearchController.java` y `BusquedaController.java` actúan como puertas de enlace que delegan el trabajo a:

* **SparqlService**: Ejecuta la lógica de las consultas SPARQL sobre los repositorios bibliográficos.
* **DataEnrichmentService**: Realiza la integración y el mapeo de los datos externos recuperados que más tarde se devuelven al usuario.

---

## 3. Estructura del Repositorio y Componentes Core

### 3.1. Organización del Código Fuente (Java)

El código se estructura bajo el paquete `com.example.demo` siguiendo un patrón de diseño por capas:

* 📂 **Config**
    * `SecurityConfig.java`: Este es el componente crítico de Spring Security. Se encarga de filtrar las peticiones HTTP y definir las políticas de autorización. Específicamente, asegura que las rutas gestionadas por el `UsuarioController` estén protegidas, mientras permite el acceso público a las rutas de `/login` y `/register`.
* 📂 **Controllers**
    * Incluye controladores de sesión (`AuthController`), navegación (`HomeController`) y servicios funcionales (`UsuarioController`, `SearchController`, `BusquedaController`).
* 📂 **Models y Repositories**
    * Contiene las entidades de dominio `Usuario` y `Busqueda`.
    * Incluye las interfaces de persistencia `UsuarioRepository` y `BusquedaRepository`.
* 📂 **Services**
    * *Lógica de Negocio*: `UserService` y `BusquedaService`.
    * *Servicios de Datos*: `SparqlService` y `DataEnrichmentService`.

### 3.2. Recursos y Configuración

El directorio `src/main/resources/` centraliza los recursos estáticos y de configuración:

* **Estilos**: `static/css/search-views.css` define la identidad visual de las vistas de búsqueda.
* **Templates**: Contiene el ecosistema de vistas HTML, incluyendo las interfaces de búsqueda federada y comparativa.
* **Application Properties**: Archivo maestro de configuración del entorno y la base de datos.

### 3.3. Gestión de Dependencias

El archivo `pom.xml` es el corazón de la gestión del ecosistema. Además de orquestar la integración de librerías para la comunicación SPARQL, gestiona los *Starters* de Spring Boot para:

* **Web**: Soporte para la construcción de APIs y controladores MVC.
* **Security**: Implementación de la capa de autenticación y autorización.
* **Data JPA**: Automatización de la capa de persistencia y conectividad con PostgreSQL.