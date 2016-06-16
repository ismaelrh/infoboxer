'use strict';

angular.module('infoboxer')

    .service('MulticategoryService', ['$http', 'DataObtainingService', '$q', 'MonocategoryService', 'AppStateService','ConfigService',
        function ($http, DataObtainingService, $q, MonocategoryService, AppStateService,ConfigService) {

            var self = this;


            //input: list of lists of handlers, one for each class
            //output: list of propertyHandlers, mixed by mode 0 (no merge)
            //Must return a promise, even if it is not necesary
            var flattenHandlersLists = function (handlersLists) {
                var propertyHandlers = []; //This will be the returned result

                for (var i = 0; i < handlersLists.length; i++) {
                    var currentList = handlersLists[i];
                    for (var j = 0; j < currentList.length; j++) {
                        propertyHandlers.push(currentList[j]);
                    }
                }
                return propertyHandlers;
            };


            /*It merges two or more propertyHandlers of the same property.
             *INPUT:  propertyHandlers -> Array of two or more propertyHandlers with same propertyURI.
             *        mode -> multicategory mode.
             *        retrievedProperty -> retrieved property backend with info about the count, label and semantic state
             *        of the combination for that property.
             *        instanceCount -> Count of instances of at least one of the categories
             *OUTPUT: A propertyHandler object containing the merging result depending on the mode.
             */
            var mergePropertyHandlers = function (propertyHandlers, mode, retrievedProperty, instanceCount) {



                //The first propertyHandler is used as a base.
                var baseHandler = propertyHandlers[0];

                //Add Property objects to baseHandler
                for (var i = 1; i < propertyHandlers.length; i++) {

                    var propertyToAdd = propertyHandlers[i].properties[0]; //Only has one property, is a mono-category handler
                    baseHandler.properties.push(propertyToAdd);

                    //isSemantic = isSemantic && propertyToAdd.semantic; //If one property is no-semantic => Result is no-semantic

                }
                baseHandler.semantic = retrievedProperty.semantic;
                baseHandler.categoriesCount = instanceCount; //categoriesCount updated
                baseHandler.instanceCount = retrievedProperty.count; //Instance count update for the combination


                if (mode === 1) {
                    //popularity = (number of instances that manifest P in list of classes / numer of instances in list of classes)
                    baseHandler.popularity = baseHandler.instanceCount / baseHandler.categoriesCount;
                }
                else if (mode === 2) {
                    //popularity = max(popularity of the property for all the categories)
                    var maxPop = 0;
                    for (var i = 0; i < baseHandler.properties.length; i++) {
                        var currentProp = baseHandler.properties[i];
                        var currentPopularity = currentProp.instanceCount / currentProp.category.categoryCount;
                        if (currentPopularity > maxPop) {
                            maxPop = currentPopularity;
                        }
                    }
                    baseHandler.popularity = maxPop;
                }
                else {
                    console.error("Only modes 0,1 and 2 are implemented.");
                }

                baseHandler.ranges = [];

                return baseHandler;

            };

            /* MULTICATEGORY DATA LOADER. Receives a class list and a mode, retrieves data and returns a list
             * of propertyHandlers. Multicategory loader.
             * INPUT:  classList-> list of classes (URI's)
             * OUTPUT: mode -> multicategory mode
             */

            self.loadData = function (classList, complexClassList, mode,semantic) {

                var result = []; //Array of propertyHandlers (result)

                var groupedList;
                var combinationCount;
                //First, retrieve data for all the categories
                var monoCategoryPromises = [];
                AppStateService.clearCategoryList();
                AppStateService.clearComplexCategoryList();


                function compareCats(a, b) {
                    if (a < b)
                        return -1;
                    if (a > b)
                        return 1;
                    return 0;
                }

                classList.sort(compareCats);


                for (var i = 0; i < classList.length; i++) {
                    AppStateService.addToCategoryList(classList[i]);

                    //Search name of that class
                    var found = false;
                    var categoryName = "";
                    for(var j = 0; j < complexClassList.length && !found;j++){
                        if(complexClassList[j]._id == classList[i]){
                            found = true;
                            categoryName = complexClassList[j].name;
                        }
                    }
                    var promise = MonocategoryService.loadCategory(classList[i],categoryName, AppStateService.getSemantic());
                    monoCategoryPromises.push(promise);
                }

                for (var i = 0; i < complexClassList.length; i++) {
                    AppStateService.addToComplexCategoryList(complexClassList[i].name, complexClassList[i]._id);
                }




                //We return a promise (so the caller can chain it ;p )
                return $q.all(monoCategoryPromises)
                    .then(function (handlersLists) {
                        //When all category data has been loaded, we have a list of list of handlers,
                        //one list for each class


                        var flatList = flattenHandlersLists(handlersLists); //List of property Handlers


                        if (mode === 0) {
                            //Mode0 -> no merge. We return here.

                            //First, we delete repeated thumbnail properties
                            var numberOfThumbnails = 0;

                            //Remove repeated thumbnail property
                            for (var k = 0; k < flatList.length; k++) {
                                if (flatList[k].propertyURI.indexOf(ConfigService.thumbnail.property) > -1) {
                                    if (numberOfThumbnails > 0) {
                                        //Only one
                                        flatList.splice(k, 1);
                                        k--;
                                    }
                                    numberOfThumbnails++;
                                }
                            }
                            return flatList;
                            //End if mode = 0
                        }
                        else{
                            //Mode 1 or 2 -> continue
                            //Object, where every key is a propertyURI, and its value is an array of propertyHandlers.
                            groupedList = _.chain(flatList).groupBy("propertyURI")._wrapped;

                            //Number of instances for the combination
                            return DataObtainingService.getInstanceCount(classList)
                                .then(function(count){
                                    //Get all properties and its count
                                    combinationCount = count;

                                    return DataObtainingService.getPropertiesList(classList,semantic);
                                })
                                .then(function(retrievedProperties){

                                    //For every retrieved property, we merge the properties with the already existent handlers.
                                    for(var i = 0; i < retrievedProperties.length;i++){

                                        var property = retrievedProperties[i];
                                        var currentHandlers = groupedList[property._id];
                                        if(currentHandlers && currentHandlers.length == 1){
                                            //No need to merge, push it inmediately (The count will be the same)
                                            result.push(currentHandlers[0]);
                                        }

                                        else if(currentHandlers && currentHandlers.length > 1){
                                            //Merge the currentHandlers, and use the retrieved count
                                            result.push(mergePropertyHandlers(currentHandlers,mode,property,combinationCount));
                                        }
                                        else{
                                            console.log("WARNING: The property " + property._id + " is not in previous categories!")
                                        }

                                    }


                                    //Obtain in one request all the ranges and their uses for all the properties of the multiple classes
                                    if (mode != 0) {
                                        return self.obtainMultiRangeCount(result);
                                    }
                                    else {
                                        return $q.when(result);
                                    }


                                })
                                .then(function (handlerList) {

                                    return handlerList;

                                })
                        }





                    })






            };

            /**
             * Calls to the multiRange service, obtains a list of properties, and for each one, a list of its ranges and their uses.
             * Then, it attachs every range list to the proper handler.
             */
            self.obtainMultiRangeCount = function (handlerList) {

                var resultList = handlerList;

                return DataObtainingService.getRangesAndUses(AppStateService.getCategoryList())
                    .then(function (data) {

                        //For every property retrieved
                        for (var i = 0; i < data.length; i++) {
                            var currentProperty = data[i];
                            var found = false;
                            var j = 0;
                            //We search for the handler in handlerList of that property
                            while (!found && j < resultList.length) {
                                var currentHandler = resultList[j];
                                if (currentProperty.key == currentHandler.propertyURI) {
                                    //Attach ranges and uses.
                                    currentHandler.ranges = currentProperty.value;

                                    //For each property, its aggregated rangeUses is the sum of the uses of every range.
                                    var rangeUsesCount = 0;
                                    for(var k = 0; k < currentHandler.ranges.length; k++){
                                        var currentRange = currentHandler.ranges[k];
                                        rangeUsesCount+=currentRange.count;
                                    }
                                    currentHandler.useCount  =  rangeUsesCount;
                                    found = true;
                                }
                                j++;
                            }

                        }


                        /**
                         * Para trabajar con interseccion. DESCOMENTAR PARA INTERSECCION
                         * Miramos todas las propiedades, y al rango de su handler le metemos los rangos de las de por debajo,
                         * si todavia no estan, pero con cuenta 0.
                         */
                        /*

                         var handlerContainsRange = function(handler,rangeURI){
                         var found = false;
                         for(var j = 0; j < handler.ranges.length && !found;j++){
                         if(handler.ranges[j]._id == rangeURI){
                         found = true;
                         }
                         }
                         return found;
                         };



                         for(var i = 0; i < resultList.length;i++){
                         var currentHandler = resultList[i];
                         for(var j = 0; j< currentHandler.properties.length;j++){
                         var currentSubproperty = currentHandler.properties[j];
                         for(var k = 0; k < currentSubproperty.ranges.length;k++){
                         var currentRange = currentSubproperty.ranges[k];
                         if(!handlerContainsRange(currentHandler,currentRange._id) && currentRange._id.indexOf("unknownRDFType")==-1){
                         console.log("found");
                         currentHandler.ranges.push({_id:currentRange._id,count:0,label:currentRange.label,semantic:false,comment:null})
                         }
                         }
                         }
                         }

                         */


                        return resultList;
                    });

            }


        }]);
