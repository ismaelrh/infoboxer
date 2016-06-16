'use strict';

/**
 * Main module of the application.
 */

angular
    .module('infoboxer', [
        'ngAnimate',
        'ngCookies',
        'ngResource',
        'ngRoute',
        'ngSanitize',
        'ngTouch',
        'ui.bootstrap',
        'angularSpinner',
        'siyfion.sfTypeahead',
        'angular-inview',
        'pascalprecht.translate',
        'ngclipboard',
        'akoenig.deckgrid',
        'LocalStorageModule'
    ])


    .config(function ($routeProvider, $translateProvider, $httpProvider, $windowProvider, $locationProvider) {


        //Interceptor for showing a message when a communication error happens.
        $httpProvider.interceptors.push('NetworkErrorsInterceptor');

        $routeProvider
            .when('/app/:cat1?/:cat2?/:pageName?', {
                templateUrl: 'views/main.html',
                controller: 'MainCtrl',
                controllerAs: 'main',
                reloadOnSearch: false
            })
            .when('/login/:cat1?/:cat2?/:pageName?', {
                templateUrl: 'views/login.html',
                controller: 'LoginCtrl',
                controllerAs: 'ctrl'
            })
            .when('/wiki/:person/:availableMinutes', {
                templateUrl: 'views/wiki.html',
                controller: 'WikiCtrl',
                controllerAs: 'ctrl'
            })

            .otherwise({
                redirectTo: '/login'
            });


        $translateProvider.translations('es', {

                /* Login screen */
                INFOBOXER_SHORT_SUBTITLE: '',
                USER_INPUT_PLACEHOLDER: 'Usuario de Wikimedia',
                START_BUTTON_TEXT: "Comenzar",
                LOGIN_FOOTER: "Versión experimental, puede contener (y contiene) bugs.",
                DATASET_WARNING: "El idioma elegido no cambiará el dataset usado.",
                CURRENT_DATASET: "Dataset actual",
                SIMPLE_GUI: "Interfaz Simple",
                COMPLETE_GUI: "Interfaz Completa",
                PLEASE_ENTER_USERNAME: "Introduzca un usuario por favor.",
                ONLY_STATISTIC: "Sólo estadístico",
                STATISTIC_AND_SEMANTIC: "Estadístico y semántico",
                LOAD_TIME_WARNING: "Algunas combinaciones de categorías puede que no estén cargadas previamente. En ese caso puede llevar hasta 10 minutos cargar los datos por primera vez.",

                /* Nav bar */
                INFOBOXER_CREATION: 'Creación Infoboxer',
                CREATE_NEW: 'Crear nuevo',
                LOGOUT: 'Salir sin guardar',
                USER: 'Usuario',
                ARE_YOU_SURE_RESET: '¿Estás seguro? Se perderán los datos introducidos.',
                SAVE_AND_FINISH: 'Guardar y finalizar',
                VIEW_SIMPLE_MODE: 'Ver modo simple',
                VIEW_COMPLEX_MODE: 'Ver modo experto',

                /* Infoboxer creation header (category, etc) */
                INFOBOXER_LONG_SUBTITLE: 'Infoboxer: Usando conocimiento estadístico y semántico para ayudar a la creación de infoboxes de Wikipedia',
                INFOBOXER_SIMPLE_SUBTITLE: 'Infoboxer: Sistema de ayuda a la creación de infoboxes',
                ENTER_NAME: 'Introduce el nombre de la instancia cuyo Infoboxer quieres crear:',
                PAGE_NAME: 'Nombre de página:',
                EXAMPLE_PAGE: 'Página de ejemplo',
                CATEGORIES: 'Categorías',
                ADD: 'Añadir',
                LOAD_DATA: 'Cargar datos',
                CATEGORY_PLACEHOLDER: 'Ejemplo: Científica',
                PAGE_PLACEHOLDER: 'Ejemplo: Margarita Salas',
                REPEATED_NOT_ALLOWED: 'Error: No se permiten categorías repetidas',
                ONLY_FROM_LIST: 'Error: Por favor, elige categorías de la lista',
                TYPE_TO_SHOW_MORE: 'Escriba para mostrar más...',
                NO_MATCH: 'No hay coincidencias',


                /* Infoboxer creation properties and data */
                PROPERTIES: 'Propiedades',
                ONLY_APPEARS_IN: 'Solo aparece en',
                APPEARS_IN: 'Aparece en',
                VALUE: 'Valor',
                AGGREGATED: 'Agregado',
                EXPORT_HTML: 'Exportar a HTML',
                EXPORT_RDF: 'Exportar a RDF',
                EXPORT_INFOBOX: 'Generar código Infobox',
                TYPE: 'Escribir',
                SHOW: 'Mostrar',
                MORE: 'más',
                REMAINING: 'restantes',
                SEMANTIC_PROPERTY: 'Propiedad semántica',
                USED_BY: 'usada por',
                INSTANCES_IN_WHOLE_DATASET: 'instancias en todo el conjunto de datos',
                CLICK_TO_ADD_IMAGE: 'Click para añadir una imagen',
                IMAGE_URL: 'URL de la imagen',
                CAN_NOT_LOAD_IMAGE: 'No se puede cargar la imagen. Elige otra por favor.',
                POPULATE_FROM_WIKIPEDIA: 'Cargar Infobox desde página de Wikipedia',
                ENTER_WIKIPEDIA_URL: 'URL página Wikipedia',
                POPULATE: 'Poblar',
                POPULATE_PLACEHOLDER: 'https://es.wikipedia.org/wiki/Margarita_Salas',
                SEMANTIC: 'semántica',
                EXTRACTED_FROM_SEMANTIC: 'Rango extraído a partir de información semántica',


                /* Auto-complete */
                INSTANCES: 'instancias',
                TIMES: 'veces',
                LABEL_BASKETBALL_PLAYER: 'Jugador/a de baloncesto',
                LABEL_SOCCER_PLAYER: 'Jugador/a de fútbol',

                /* Generators */
                INFOBOX_GENERATION_FOOTER: 'Este código no puede importarse directamente a Wikipedia. Tendrás que editar aspectos como la plantilla de Infobox usada'
                + ' y adecuar los atributos a los de dicha plantilla. Más información en https://en.wikipedia.org/wiki/Help:Infobox',

                /* Finish screen */
                NEXT: 'Siguiente',
                FINISH: 'Terminar',
                HERE_IS_INFOBOXER_CODE: 'Aquí tienes tu código de Infobox',
                BEFORE_YOU_GO: 'Antes de que te vayas, ¿podrías responder a unas preguntas?',
                USE_EASINESS: 'Facilidad de uso',
                CREATION_SPEED: 'Rapidez al crear el Infobox',
                DOUBTS: 'Dudas surgidas',
                LITTLE: 'Poca',
                NORMAL: 'Normal',
                A_LOT: 'Mucha',
                A_FEW: 'Pocas/ninguna',
                SOME: 'Alguna',
                LOTS: 'Muchas',
                MORE_COMMENTS: '¿Quieres añadir algún comentario más?',
                NO_COMMENTS: 'Sin comentarios.',
                ITS_FINISHED: '¡Se acabó!',
                FINISHED_MESSAGE: 'Al pulsar finalizar, irás a la pantalla de inicio.',
                INFOBOX_FOR: 'Infobox para',
                INFOBOX_CODE: 'Código de Infobox',
                ERROR_OCCURRED: 'Ha ocurrido un error',
                COPY: 'Copiar',
                LOADING: 'Cargando',
                /** About **/
                ABOUT_TITLE: 'Acerca de Infoboxer',
                ABOUT_DESCRIPTION: 'Infoboxer usa conocimiento estadístico y semántico de fuentes de datos relacionadas enlazadas' +
                ' para facilitar el proceso de creación de Infoboxes de Wikipedia. Crea plantillas dinámicas y semánticas' +
                ' sugiriendo atributes comunes en artículos similares y controlando los valores esperados de forma semántica.',
                INTEGRATORS_UNIZAR_TITLE: 'Integrantes (Universidad de Zaragoza)',
                INTEGRATORS_UMBC_TITLE: 'Integrantes (UMBC)',
                ABOUT_MORE: 'Puedes encontrar más información en http://sid.cps.unizar.es/Infoboxer/',


                /* Common strings */
                CANCEL: 'Cancelar',
                FEATURE_NOT_AVAILABLE: 'Funcionalidad no disponible todavía.',
                SAVE_AND_APPLY: 'Guardar y aplicar',

                /** Configuration **/
                CONFIGURATION: 'Configuración',
                CONFIG_MESSAGE: 'Aquí puedes configurar algunos aspectos de presentación',
                INTERFACE_LANGUAGE: 'Idioma de interfaz',
                INTERFACE_MODE: 'Modo de interfaz',
                PROPERTIES_MODE: 'Modo de propiedades',
                CONFIG_FOOTER: "Cambios hechos al 'modo de propiedades' necesitan que recargues los datos para aplicarse",
                SIMPLE: "Simple",
                COMPLEX: "Completo",
                IE_WARNING: "Infoboxer NO FUNCIONA correctamente con Internet Explorer ni Edge. Por favor, usa Google Chrome o Mozilla Firefox"


            })
            .translations('en', {

                /* Login screen */
                INFOBOXER_SHORT_SUBTITLE: 'Infoboxer creation helper system.',
                USER_INPUT_PLACEHOLDER: 'Wikimedia username',
                START_BUTTON_TEXT: "Start",
                LOGIN_FOOTER: "Experimental version, may (and does) contain bugs.",
                DATASET_WARNING: "Selected language won't change the used dataset.",
                CURRENT_DATASET: "Current dataset",
                SIMPLE_GUI: "Simple GUI",
                COMPLETE_GUI: "Complete GUI",
                PLEASE_ENTER_USERNAME: "Please, specify an username.",
                ONLY_STATISTIC: "Only statistic",
                STATISTIC_AND_SEMANTIC: "Statistic and semantic",
                LOAD_TIME_WARNING: "Some combination of categories may not be loaded. If so, please be patient, it can take up to 10 minutes the first time.",

                /* Nav bar */
                INFOBOXER_CREATION: 'Infoboxer creation',
                CREATE_NEW: 'Create new',
                LOGOUT: 'Exit without saving',
                USER: 'User',
                ARE_YOU_SURE_RESET: 'Are you sure? Entered data will be lost.',
                SAVE_AND_FINISH: 'Save and finish',
                VIEW_SIMPLE_MODE: 'View simple mode',
                VIEW_COMPLEX_MODE: 'View complete mode',

                /* Infoboxer creation header (category, etc) */
                INFOBOXER_LONG_SUBTITLE: 'Infoboxer: Using Statistical and Semantic Knowledge to Help Creating Wikipedia Infoboxes',
                INFOBOXER_SIMPLE_SUBTITLE: 'Infoboxer: Wikipedia Infoboxer creation helper system.',
                ENTER_NAME: 'Enter the instance name whose infobox you want to create:',
                PAGE_NAME: 'Page name:',
                EXAMPLE_PAGE: 'Example page',
                CATEGORIES: 'Categories',
                ADD: 'Add',
                LOAD_DATA: 'Load data',
                CATEGORY_PLACEHOLDER: 'E.g: SoccerPlayer',
                PAGE_PLACEHOLDER: 'E.g: David Beckham',
                REPEATED_NOT_ALLOWED: 'Error: No repeated categories allowed',
                ONLY_FROM_LIST: 'Error: Please, select categories from the list',
                TYPE_TO_SHOW_MORE: 'Type to show more...',
                NO_MATCH: 'No matches',

                /* Infoboxer creation properties and data */
                PROPERTIES: 'Properties',
                ONLY_APPEARS_IN: 'Only appears in',
                APPEARS_IN: 'Appears in',
                VALUE: 'Value',
                AGGREGATED: 'Aggregated',
                EXPORT_HTML: 'Export to HTML',
                EXPORT_RDF: 'Export to RDF',
                EXPORT_INFOBOX: 'Generate Infobox code',
                TYPE: 'Type',
                SHOW: 'Show',
                MORE: 'more',
                REMAINING: 'remaining',
                SEMANTIC_PROPERTY: 'Semantic property',
                USED_BY: 'used by',
                INSTANCES_IN_WHOLE_DATASET: 'instances in the whole dataset',
                CLICK_TO_ADD_IMAGE: 'Click to add an image',
                IMAGE_URL: 'Image URL',
                CAN_NOT_LOAD_IMAGE: 'Can\'t load image. Please select another one.',
                POPULATE_FROM_WIKIPEDIA: 'Load Infobox from Wikipedia page',
                ENTER_WIKIPEDIA_URL: 'Enter Wikipedia page URL',
                POPULATE: 'Populate',
                POPULATE_PLACEHOLDER: 'https://en.wikipedia.org/wiki/David_Beckham',
                SEMANTIC: 'semantic',
                EXTRACTED_FROM_SEMANTIC: 'Range extracted from semantic information',

                /* Auto-complete */
                INSTANCES: 'instances',
                TIMES: 'times',
                LABEL_BASKETBALL_PLAYER: 'Basketball Player',
                LABEL_SOCCER_PLAYER: 'Soccer Player',

                /* Generators */
                INFOBOX_GENERATION_FOOTER: 'This code cannot be imported to Wikipedia without changes. You must edit aspects like used Infobox template'
                + ' and adapt attributes to the ones of that template. See more at https://en.wikipedia.org/wiki/Help:Infobox',

                /* Finish screen */
                NEXT: 'Next',
                FINISH: 'Finish',
                HERE_IS_INFOBOXER_CODE: 'Here it is your Infobox code',
                BEFORE_YOU_GO: 'Before you go, could you answer a few questions?',
                USE_EASINESS: 'Use easiness',
                CREATION_SPEED: 'Infobox creation speed',
                DOUBTS: 'Doubts',
                LITTLE: 'Little',
                NORMAL: 'Normal',
                A_LOT: 'A lot',
                A_FEW: 'Few/none',
                SOME: 'Some',
                LOTS: 'Lots',
                MORE_COMMENTS: 'Do you want to add an additional comment?',
                NO_COMMENTS: 'No comments.',
                ITS_FINISHED: 'It\'s finished!',
                FINISHED_MESSAGE: 'By pressing "Finish", you will be redirected to start page.',
                INFOBOX_FOR: 'Infobox for',
                INFOBOX_CODE: 'Infobox code',
                ERROR_OCCURRED: 'An error has occurred',
                COPY: 'Copy',
                LOADING: 'Loading',


                /** About **/
                ABOUT_TITLE: 'About Infoboxer',
                ABOUT_DESCRIPTION: 'Infoboxer uses statistical and semantic knowledge from linked data sources' +
                ' to ease the process of creating Wikipedia infoboxes. It creates dynamic and semantic templates' +
                ' by suggesting attributes common for similar articles and controlling the expected values semantically.',
                INTEGRATORS_UNIZAR_TITLE: 'Integrators (University of Zaragoza)',
                INTEGRATORS_UMBC_TITLE: 'Integrators (UMBC)',
                ABOUT_MORE: 'More information can be found at http://sid.cps.unizar.es/Infoboxer/',

                /* Common strings */
                CANCEL: 'Cancel',
                FEATURE_NOT_AVAILABLE: 'Feature not available yet',
                SAVE_AND_APPLY: 'Save and apply',


                /** Configuration **/
                CONFIGURATION: 'Configuration',
                CONFIG_MESSAGE: 'Here you can config some presentation aspects',
                INTERFACE_LANGUAGE: 'Interface language',
                INTERFACE_MODE: 'Interface mode',
                PROPERTIES_MODE: 'Properties mode',
                CONFIG_FOOTER: "Changes to 'properties mode' need you to reload the data to take effect.",
                SIMPLE: "Simple",
                COMPLEX: "Complete",
                IE_WARNING: "Infoboxer DOES NOT WORK properly with Internet Explorer and Edge. Please use Google Chrome or Mozilla Firefox."


            });


        //Detect language from browser
        var $window = $windowProvider.$get();
        var lang = $window.navigator.language || $window.navigator.userLanguage;
        if (lang.indexOf("en") >= 0) {
            $translateProvider.preferredLanguage('en');
        }
        else if (lang.indexOf("es") >= 0) {
            $translateProvider.preferredLanguage('es');
        }
        else {
            $translateProvider.preferredLanguage('en');
        }


    });
