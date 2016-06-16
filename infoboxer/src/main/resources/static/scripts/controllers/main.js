'use strict';

/**
 * @ngdoc function
 * @name infoboxer.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the infoboxer
 */
angular.module('infoboxer')
    .controller('MainCtrl', [
        '$http', 'ToolsService', 'MonocategoryService', 'MulticategoryService', 'InfoModalService', 'OutputDataService',
        'AppStateService', 'usSpinnerService', '$location', '$route', 'StatsService', '$window', '$translate', 'UserFinishModalService',
        '$rootScope', 'AboutModalService', '$timeout', 'DataObtainingService', 'removeSpacesFilter', '$anchorScroll', '$routeParams', 'ConfigService', 'ConfigModalService','$scope',
        function ($http, ToolsService, MonocategoryService, MulticategoryService, InfoModalService, OutputDataService,
                  AppStateService, usSpinnerService, $location, $route, StatsService, $window, $translate, UserFinishModalService,
                  $rootScope, AboutModalService, $timeout, DataObtainingService, removeSpacesFilter, $anchorScroll, $routeParams, ConfigService, ConfigModalService,$scope) {

            var self = this;

            self.config = ConfigService;


            self.limit = 30;
            self.pagination = 30;

            self.shownAlerts = [];
            self.currentAlerts = [];
            self.inputPrueba = "";
            self.editingPhoto = false;
            self.wikipediaInstance = "";

            AppStateService.loadFromLocalStorage();
            self.language = $translate.use();
            self.semantic = AppStateService.getSemantic();
            self.complex = !AppStateService.getSimplified();


            $http.get('data/categories.json')
                .then(function (res) {
                    self.categories = res.data;

                    //When categories are loaded, we parse the parameters to pre-populate category fields

                    var cat1 = $routeParams.cat1;
                    var cat2 = $routeParams.cat2;
                    var pageName = $routeParams.pageName;

                    var hasParams = false;

                    //Search first category parameter
                    for (var i = 0; i < self.categories.length && cat1; i++) {
                        if (self.categories[i].name.toUpperCase() === cat1.toUpperCase()) {

                            hasParams = true;

                            if (self.classList[0].name && self.classList[0].name.length > 0) {
                                self.classList.push({
                                    "_id": self.categories[i]["_id"],
                                    "name": self.categories[i]["name"]
                                });
                            }
                            else {
                                self.classList = [{
                                    "_id": self.categories[i]["_id"],
                                    "name": self.categories[i]["name"]
                                }];
                            }
                        }
                    }

                    //Search second category parameter
                    if(cat2 && cat2.toUpperCase() != "EMPTY"){
                        for (var i = 0; i < self.categories.length && cat2; i++) {
                            if (self.categories[i].name.toUpperCase() === cat2.toUpperCase()) {

                                hasParams = true;
                                if (self.classList[0].name && self.classList[0].name.length > 0) {
                                    self.classList.push({
                                        "_id": self.categories[i]["_id"],
                                        "name": self.categories[i]["name"]
                                    });
                                }
                                else {
                                    self.classList = [{
                                        "_id": self.categories[i]["_id"],
                                        "name": self.categories[i]["name"]
                                    }];
                                }
                            }
                            }

                    }

                    if(pageName && pageName.length > 0){
                        self.pageName = pageName;
                    }

                    //Load data
                    if (hasParams) {
                        console.log("Automatically loading data...");
                        self.loadData();
                    }


                });


            /** Network Warning and error listener. It receives broadcasts from Network Errors
             Interceptor and shows alerts. Not repeated.
             */
            $rootScope.$on('networkWarning', function (events, args) {

                var found = false;
                for (var i = 0; i < self.shownAlerts.length && !found; i++) {

                    var ca = self.shownAlerts[i];
                    if (ca.message == args.message) {
                        found = true;
                    }
                }
                if (!found) {
                    //Not shown previously
                    var alert = {type: args.type, title: "Warning:", message: args.message};
                    self.currentAlerts.push(alert);
                    self.shownAlerts.push(alert);
                }


            });

            $rootScope.$on('complex-changed', function (event, args) {

                self.complex = !AppStateService.getSimplified();

            });


            $rootScope.$on('semantic-changed', function (event, args) {

                self.semantic = AppStateService.getSemantic();

            });

            self.incrementLimit = function () {
                self.limit += self.pagination;
            };

            self.switchComplex = function () {
                self.complex = !self.complex;
                AppStateService.setSimplified(!self.complex);
            };


            self.pageName = ""; //Page name
            self.propertyHandlers = []; //Contains current property handlers
            self.classList = [{}]; //This is current displayed classList
            self.infoboxOnScreen = false; //Indicates if the Infobox section title is on screen
            self.isma = false;


            /* angular.element($window).bind("beforeunload", function (event) {

             return 'Atención';
             });*/


            self.isRestrictedByNumber = function(){
                return ConfigService.categoriesRestrictions.restrictByNumber;
            };

            self.maxNumberOfCategories = function(){
                return ConfigService.categoriesRestrictions.maxNumber;
            };

           /* self.modes = [{  //Available multi-category modes
                name: 'No merge',
                value: 0
            }, {
                name: 'Merge - Sort by frequency',
                value: 1
            }, {
                name: 'Merge - Sort by max of frequencies',
                value: 2
            }];*/
            self.modes = [{  //Available multi-category modes
                name: 'No merge',
                value: 0
            }, {
                name: 'Merge',
                value: 1
            }
            ];
            self.currentMode = self.modes[1]; //Current mode

            // Typeahead options object

            self.typeaheadOptions = {
                highlight: true,
                hint: true,
                minLength: 0
            };


            self.getUsername = function () {
                return StatsService.username;
            }

            self.getSessionId = function () {
                return StatsService.sessionId;
            }

            self.addAction = function (subject, action, value) {
                $timeout(function () {
                    StatsService.addAction(subject, action, value); //No nos preocupamos del retorno, es asíncrono.
                }, 500);

            }


            //Used for property values field. Wait for 500ms because text doesnt update instantly.
            self.addOutFocusValueAction = function (subject, action, value) {
                $timeout(function () {
                    StatsService.addAction(subject, action, value.text); //No nos preocupamos del retorno, es asíncrono.
                }, 500);
            }

            self.addOutFocusValueActionClass = function (subject, action, value) {
                $timeout(function () {
                    StatsService.addAction(subject, action, value.name); //No nos preocupamos del retorno, es asíncrono.
                }, 500);
            }


            self.resetSession = function () {
                //todo: cerrar sesion anterior
                if (confirm($translate.instant("ARE_YOU_SURE_RESET"))) {
                    if (StatsService.stats_activated) {

                        //Indicate END EDITION
                        StatsService.addAction("EDITION INFORMATION", "EDITION END")
                            .then(function () {
                                //Close session
                                StatsService.closeSession();
                            })
                            .then(function () {
                                //New session
                                StatsService.newSession(StatsService.username);
                            })
                            .then(function () {
                                //Indicate START EDITION
                                StatsService.addAction("EDITION INFORMATION", "EDITION START");
                                $route.reload();
                            })
                            .catch(function () {
                                $route.reload();
                            })
                    }
                    else {
                        //Stats not activated
                        $route.reload();
                    }
                }


            }

            //Called when the controller starts. If no session has been started, it starts a new one
            if (!StatsService.sessionId && StatsService.stats_activated) {
                StatsService.newSession(StatsService.username)
                    .then(function () {
                        StatsService.addAction("EDITION INFORMATION", "EDITION START");
                    });
            }
            //Called when the Infobox section title enters or exists the screen.
            //Updates the infoboxOnScreen attribute with false if it is out of the screen,
            //true otherwise
            self.setInfoboxOnScreen = function (a) {
                self.infoboxOnScreen = a;

            }


            this.go = function (path) {
                $location.path(path);
            };

            //Logout function
            this.logout = function () {
                if (confirm($translate.instant("ARE_YOU_SURE_RESET"))) {

                    if (StatsService.stats_activated) {
                        //Indicate END EDITION
                        StatsService.addAction("EDITION INFORMATION", "EDITION END")
                            .then(function () {
                                //Indicate to close session
                                StatsService.closeSession();
                            });

                    }

                    this.go('/login');
                }

            }


            self.addedValueField = function (handler) {
                self.addAction('BUTTON ADD VALUE ' + handler.propertyURI, 'CLICK');
            }

            self.removedValueField = function (handler, index) {
                self.addAction('BUTTON REMOVE VALUE ' + handler.propertyURI + " (index " + index + ")", 'CLICK');
            }
            /**
             Opens a google search for the property in a separated tab/window.
             */
            self.searchGoogle = function (handler) {
                self.addAction('BUTTON SEARCH', 'CLICK', handler.propertyURI);

                $window.open('https://www.google.com/search?q=' + self.pageName + "+" + handler.propertyLabel);
            };


            self.myList = [];
            self.myCategoryDataList = [];




            /**
             * For a given property handler (that can contain multiple categories),
             * it returns an object { classList, ranges,labels,semantic }, where
             * classList is a list of categories, ranges and labels are id's and labels of three most
             * popular ranges.Semantic is true if property is semantic (obtained from ontology, not data).
             * Used for passing it to rangesTypeahead.
             * It is called by every value input element, for every property.
             */
            self.getRangeData = function (handler) {


                //Check if it is already in memory
                var key = handler.propertyURI + handler.getCategoriesURIsList();
                if (self.myCategoryDataList[key]) {
                    return self.myCategoryDataList[key];
                }
                else {

                    //Prepare object
                    var object = {
                        classList: handler.getCategoriesURIs(),
                        ranges: [],
                        labels: [],
                        semantic: handler.semantic
                    };

                    //Only three ranges
                    for (var i = 0; i < handler.ranges.length && i < 4; i++) {
                        object.ranges.push(handler.ranges[i]._id);
                        object.labels.push(handler.ranges[i].label);
                    }

                    //If semantic (handler.ranges would be empty)
                    if (handler.semantic && handler.rangeForSemantic) {
                        object.ranges.push(handler.rangeForSemantic._id);
                        object.labels.push(handler.rangeForSemantic.label);
                    }

                    //Save to cache
                    self.myCategoryDataList[key] = object;
                    return object;
                }
            };


            /**
             * Normalizes class input data.
             *  It is needed, because typeahead sets the model as an object, of
             *  which we have to retrieve name.
             *  INPUT: Class object.
             */
            self.normalizeClassInput = function (clase) {

                if (clase.value._id) {
                    //Is an objects, comes from typeahead
                    clase.value = clase.value._id;
                }

            };

            /**
             * Normalizes value input data.
             *  It is needed, because typeahead sets the model as an object, of
             *  which we have to retrieve type and text.
             *  INPUT: Value object.
             */
            self.normalizeValueInput = function (value) {


                if (value.text._id) {
                    //Is an objects, comes from typeahead
                    value.type = value.text.type;
                    value.text = value.text._id;
                }
                else {
                    //Not an object, comes from user typing
                    value.text = value.text;
                    value.type = "XMLSchema#String";
                }
            };

            //Starts the spinner
            self.startSpin = function () {

                usSpinnerService.spin('spinnerLoading');
            };

            //Stops the spinner
            self.stopSpin = function () {
                usSpinnerService.stop('spinnerLoading');
            };


            //Removes class from class list. If it has only one element, clears the text.
            self.removeClassFromList = function (index) {


                if (self.classList.length == 1) {
                    self.classList[0].name = '';
                    self.addAction('BUTTON CLEAR CATEGORY (index ' + index + ")", 'CLICK');
                }
                else if (index > -1) {
                    self.classList.splice(index, 1);
                    self.addAction('BUTTON REMOVE CATEGORY (index ' + index + ")", 'CLICK');
                }


            };

            //Adds class to class list
            self.addClassToList = function () {
                self.classList.push({});
                self.addAction("BUTTON ADD CATEGORY", "CLICK");
            };


            /**
             *Loads data and processes it.
             */
            self.loadData = function () {




                //Save values
                AppStateService.clearValues();

                if (self.propertyHandlers) {
                    //Check for null
                    AppStateService.saveValues(self.propertyHandlers);
                }


                //We use only the classes that are not empty
                var allClassesWithURI = true;
                var finalClassList = [];
                var finalComplexClassList = [];


                for (var i = 0; i < self.classList.length; i++) {


                    if (self.classList[i]._id && self.classList[i]._id.length > 0) {


                        //Check repeated, only add to list if not repeated (The URI)
                        var alreadyInserted = false;
                        var j = 0;

                        while (!alreadyInserted && j < finalComplexClassList.length) {
                            alreadyInserted = (finalComplexClassList[j]._id == self.classList[i]._id);
                            j++;
                        }

                        if (!alreadyInserted) {
                            finalClassList.push(self.classList[i]._id);
                        }
                        //Always to the complex list
                        finalComplexClassList.push(self.classList[i]);
                    }
                    else {
                        console.error("Error");
                    }


                }


                var isAllowed = self.combinationIsAllowed(finalClassList)

                if (allClassesWithURI && isAllowed) {
                    //if(!ToolsService.hasDuplicates(finalClassList)){
                    if (true) { //Allow repeated
                        switch (finalClassList.length) {
                            case 0:
                                //No category supplied
                                self.addAction("BUTTON LOAD DATA", "CLICK", "ERROR: TYPE A CATEGORY");
                                alert("Please, type a category");
                                break;
                            case 1:
                                AppStateService.clearCategoryList();
                                AppStateService.clearComplexCategoryList();
                                self.addAction("BUTTON LOAD DATA", "CLICK", "MONOCATEGORY");
                                self.startSpin();
                                //mono-category
                                //Search name of category



                                MonocategoryService.loadCategory(finalComplexClassList[0]._id,finalComplexClassList[0].name, AppStateService.getSemantic()).then(function (handlers) {
                                    AppStateService.addToCategoryList(finalClassList[0]); //Add to current categories

                                    //Add to complex list
                                    for (var k = 0; k < finalComplexClassList.length; k++) {
                                        AppStateService.addToComplexCategoryList(finalComplexClassList[k].name, finalComplexClassList[k]._id);
                                    }

                                    self.propertyHandlers = handlers;

                                    AppStateService.restoreValues(self.propertyHandlers); //restore values
                                    self.stopSpin();
                                });
                                break;
                            default:
                                self.addAction("BUTTON LOAD DATA", "CLICK", "MULTICATEGORY");
                                self.startSpin();
                                //Multi-category
                                MulticategoryService.loadData(finalClassList, finalComplexClassList, self.currentMode.value,AppStateService.getSemantic()).then(function (newHandlers) {

                                    self.propertyHandlers = newHandlers;


                                    AppStateService.restoreValues(self.propertyHandlers); //restore values
                                    self.stopSpin();

                                });

                        }
                    }
                    else {
                        self.addAction("BUTTON LOAD DATA", "CLICK", "ERROR: REPEATED CATEGORIES");
                        alert($translate.instant('REPEATED_NOT_ALLOWED'));
                    }
                }
                else {

                    if (!isAllowed) {
                        self.addAction("BUTTON LOAD DATA", "CLICK", "ERROR: COMBINATION NOT ALLOWED");
                        alert("Combinación no disponible");

                    }
                    else {
                        self.addAction("BUTTON LOAD DATA", "CLICK", "ERROR: CATEGORY NOT FROM LIST");
                        alert($translate.instant('ONLY_FROM_LIST'));
                    }

                }


            };


            self.combinationIsAllowed = function (finalClassList) {

                function compareCats(a, b) {
                    if (a < b)
                        return -1;
                    if (a > b)
                        return 1;
                    return 0;
                }


                //Restrict by whitelist. Only applied when two or more categories are selected
                //Number restriction is handled by interface (disabling "Add" button)
                if (ConfigService.categoriesRestrictions.restrictByWhitelist) {

                    var allowed = ConfigService.categoriesRestrictions.whiteList;
                    if (allowed == undefined) {
                        console.error("Please define a whitelist or set 'restrictByWhitelist' to false");
                        allowed = []; //If no value is present -> Empty! No combination is allowed.
                    }

                    //Now, order each value alphabetically so we can compare
                    for(var i = 0; i < allowed.length; i++){
                        var current = allowed[i];
                        current.sort(compareCats);
                    }


                    if (finalClassList.length <= 1) {
                        //Monocategory, no problem
                        return true;
                    }
                    else {
                        //First, order category list alphabetically.
                        finalClassList.sort(compareCats);
                        var match = false;
                        var stringToCompare = "";
                        //concatenate to compare
                        for (var j = 0; j < finalClassList.length; j++) {
                            stringToCompare += finalClassList[j];
                        }

                        for (var k = 0; k < allowed.length && !match; k++) {
                            var currentString = "";
                            //Concatenate to compare
                            for (var l = 0; l < allowed[k].length; l++) {
                                currentString += allowed[k][l];
                            }

                            match = (stringToCompare == currentString);

                        }

                        //If match = true -> OK
                        //Else -> pattern not found

                        return match;

                    }

                }

                //If no restriction is set, return true
                return true;


            };
            /**
             * Returns lower values if popularity is higher. The highest
             * value is for'thumbnail' property, it has to appear the first.
             * Used to sort the Infobox.
             */
            self.sortInfobox = function (handler) {

                if (handler.propertyURI.indexOf(ConfigService.thumbnail.property) > -1) {
                    return -200;
                }
                else {
                    return -handler.popularity;
                }
            };


            self.openAboutModal = function () {
                var modalOptions = {
                    closeButtonText: $translate.instant('CANCEL'),
                    about_title: $translate.instant('ABOUT_TITLE'),
                    about_description: $translate.instant('ABOUT_DESCRIPTION'),
                    integrators_unizar_title: $translate.instant('INTEGRATORS_UNIZAR_TITLE'),
                    integrators_umbc_title: $translate.instant('INTEGRATORS_UMBC_TITLE'),
                    about_more: $translate.instant('ABOUT_MORE')
                };
                AboutModalService.showModal({}, modalOptions);
            }


            /** Opens finish modal after saving the infobox on the server (if possible)*/
            self.openFinishModal = function () {


                var givenInfobox = OutputDataService.generateInfobox(self.pageName, self.propertyHandlers);
                givenInfobox = givenInfobox.replace("\n", "\n"); //Replace line breaks with \n
                var rdfCode = OutputDataService.generateRDF(self.pageName, self.propertyHandlers);

                //Modal options
                var modalOptions = {

                    onlyInfobox: false,
                    infobox_export_screen: {
                        message: $translate.instant('HERE_IS_INFOBOXER_CODE'),
                        givenInfobox: givenInfobox,
                        pageName: self.pageName,
                        categoryList: AppStateService.getComplexCategoryList(),
                        footer: $translate.instant('INFOBOX_GENERATION_FOOTER'),
                    },

                    survey_points_screen: {
                        message: $translate.instant('BEFORE_YOU_GO'),
                        questions: [
                            {
                                "title": $translate.instant('USE_EASINESS'),
                                "value": 1,
                                footer: [$translate.instant('LITTLE'), $translate.instant('NORMAL'), $translate.instant('A_LOT')]
                            },
                            {
                                "title": $translate.instant('CREATION_SPEED'),
                                "value": 1,
                                footer: [$translate.instant('LITTLE'), $translate.instant('NORMAL'), $translate.instant('A_LOT')]
                            },
                            {
                                "title": $translate.instant('DOUBTS'),
                                "value": 1,
                                footer: [$translate.instant('LOTS'), $translate.instant('SOME'), $translate.instant('A_FEW')]
                            }
                        ]
                    },

                    survey_free_screen: {
                        message: $translate.instant('MORE_COMMENTS'),
                        text: $translate.instant('NO_COMMENTS'),
                    },

                    congrats_screen: {
                        message: $translate.instant('ITS_FINISHED'),
                        submessage: $translate.instant('FINISHED_MESSAGE'),
                    }


                };


                //Add action of pushed button
                StatsService.addAction("FINISH BUTTON", "CLICK", "")
                    .then(function () {
                        //Add action of edition finished
                        return StatsService.addAction("EDITION INFORMATION", "EDITION END")
                    })

                    .then(function () {
                        //Save infobox on server
                        var catList = AppStateService.getComplexCategoryList();
                        var stringList = "";

                        for (var i = 0; i < catList.length; i++) {
                            //CamelCased URI
                            stringList += catList[i].name + " - ";
                        }
                        //Save RDF
                        return StatsService.saveRdf(self.pageName, stringList, rdfCode);
                    });


                //SHOW DIALOG
                UserFinishModalService.showModal({}, modalOptions);


            };

            /** Opens finish modal after saving the infobox on the server (if possible)*/
            self.openInfoboxModal = function () {


                var givenInfobox = OutputDataService.generateInfobox(self.pageName, self.propertyHandlers);
                givenInfobox = givenInfobox.replace("\n", "\n"); //Replace line breaks with \n


                //Modal options
                var modalOptions = {

                    onlyInfobox: true,
                    infobox_export_screen: {
                        message: $translate.instant('HERE_IS_INFOBOXER_CODE'),
                        givenInfobox: givenInfobox,
                        pageName: self.pageName,
                        categoryList: AppStateService.getComplexCategoryList(),
                        footer: $translate.instant('INFOBOX_GENERATION_FOOTER'),
                    }

                };


                //Add action of pushed button
                StatsService.addAction("GET INFOBOX BUTTON", "CLICK", "")
                    .then(function () {
                        //Add action of edition finished
                        UserFinishModalService.showModal({}, modalOptions);
                    });


            };

            /** Generates RDF from current data and shows it on a modal dialog*/
            self.openRDFmodal = function () {
                var rdf = OutputDataService.generateRDF(self.pageName, self.propertyHandlers);
                var modalOptions = {
                    closeButtonText: $translate.instant('CANCEL'),
                    headerText: $translate.instant('EXPORT_RDF'),
                    bodyText: rdf
                };
                InfoModalService.showModal({}, modalOptions);
            };


            /** Generates RDF from current data and shows it on a modal dialog*/
            self.showConfig = function () {
                var modalOptions = {
                    closeButtonText: $translate.instant('CANCEL'),
                    headerText: $translate.instant('EXPORT_RDF')
                };
                ConfigModalService.showModal({}, modalOptions);
            };

            /** Generates Infobox HTML from current data and shows it on a modal dialog */
            self.openHTMLmodal = function () {
                var html = OutputDataService.generateHTML(self.pageName, self.propertyHandlers);
                var modalOptions = {
                    closeButtonText: $translate.instant('CANCEL'),
                    headerText: $translate.instant('EXPORT_HTML'),
                    bodyText: html
                };
                InfoModalService.showModal({}, modalOptions);
            };


            /**
             * Returns true only if a thumbnail is available among the properties.
             * @returns {boolean}
             */
            self.getThumbnail = function () {
                for (var i = 0; i < self.propertyHandlers.length; i++) {
                    if (self.propertyHandlers[i].propertyURI.indexOf(ConfigService.thumbnail.property) != -1 && self.propertyHandlers[i].values.length > 0) {
                        return self.propertyHandlers[i].values[0].text;
                    }
                }
                return "/images/addimage.png";
            };


            /**
             * Loads values for a instance of Wikipedia in dataset,
             * and displays the values on the fields.
             */
            self.loadWikipediaInstance = function () {


                self.wikipediaInstance = decodeURIComponent(self.wikipediaInstance);

                //1.- Get data from web-service
                DataObtainingService.getDataForInstance(self.wikipediaInstance)
                    .then(function (response) {

                            //2.- Iterate over the retrieved properties and assign them to the current handlers
                            for (var i = 0; i < response.length; i++) {
                                var currentProperty = response[i];
                                var found = false;
                                for (var j = 0; j < self.propertyHandlers.length && !found; j++) {
                                    var currentHandler = self.propertyHandlers[j];
                                    if (currentProperty.key == currentHandler.propertyURI) {
                                        //Property match. We add the retrieved values to the currentHandler's values

                                        for (var k = 0; k < currentProperty.value.length; k++) {
                                            var currentValue = currentProperty.value[k];
                                            if (currentHandler.values.length == 1 && currentHandler.values[0].text.length == 0) {
                                                //If currentHandler value's are empty (only one property with no text) -> delete it
                                                //to avoid leaving one field empty.
                                                currentHandler.values = [];
                                            }
                                            currentHandler.values.push({
                                                text: currentValue.label,
                                                uri: currentValue.uri,
                                                type: currentValue.type,
                                                changedByWiki: true
                                            });


                                        }


                                    }

                                }

                            }

                            self.wikipediaInstance = "";
                            self.showPopulate = false;
                        }
                    );

            };


            self.transformUriToPrefix = function (oldUri, newPrefix) {
                return ToolsService.transformUriToPrefix(oldUri, newPrefix);
            };

            self.goToProperty = function (property) {
                var prop = "propertyBox-" + removeSpacesFilter(property);
                $anchorScroll.yOffset = 70;
                $location.hash(prop);
                $anchorScroll();
            };


        }]);

