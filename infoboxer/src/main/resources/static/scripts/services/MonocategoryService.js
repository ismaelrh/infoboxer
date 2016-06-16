'use strict';

angular.module('infoboxer')

    .service('MonocategoryService', ['$http', 'DataObtainingService', '$q', function ($http, DataObtainingService, $q) {


        //Returns a promise that calls the callback with an array of PropertyHandler objects.
        this.loadCategory = function (categoryURI,categoryName, semantic) {

            var category = new Category(categoryURI,categoryName);

            var handlers = [];
            var rangeIsBeingCalculated = [];
            //We return the promise, so the controller can call the "then" function
            //and do things when the category has been loaded

            return DataObtainingService.getInstanceCount([categoryURI]).then(function (categoryCount) {
                //Retrieve number of instances of category
                category.categoryCount = categoryCount;

                //Obtain list of properties
                return DataObtainingService.getPropertiesList([categoryURI], semantic);
            }).then(function (propertyList) {



                var propertyNumber = propertyList.length;
                var rangePromises = []; //Stores promises of API calls

                //For each property retrieved, we create property and propertyHandler objects
                for (var i = 0; i < propertyNumber; i++) {
                    var currentCount = propertyList[i];

                    var property = new Property();
                    var propertyHandler = new PropertyHandler();

                    /*Fill property object*/
                    property.propertyURI = currentCount._id;
                    property.instanceCount = currentCount.count;
                    property.propertyLabel = currentCount.label;
                    property.propertyComment = currentCount.comment;


                    property.category = category;
                    property.popularity = property.instanceCount / category.categoryCount;
                    property.semantic = currentCount.semantic;
                    if (!property.semantic) {
                        property.semantic = false;
                    }


                    property.rangeForSemantic = currentCount.rangeForSemantic;


                    //useCount will be filled in next step

                    /*Fill propertyHandler object*/
                    propertyHandler.propertyURI = currentCount._id;
                    propertyHandler.categoriesCount = category.categoryCount;
                    propertyHandler.instanceCount = property.instanceCount;
                    propertyHandler.propertyLabel = currentCount.label;
                    propertyHandler.values.push(new Value("", "XMLSchema#String"));
                    propertyHandler.properties.push(property);
                    propertyHandler.popularity = property.popularity;
                    propertyHandler.semantic = property.semantic;
                    propertyHandler.propertyComment = property.propertyComment;
                    propertyHandler.rangeForSemantic = property.rangeForSemantic;

                    handlers.push(propertyHandler);
                    //We push the API call promise for obtaining range and uses




                }
                //todo: crear otra operacion con otro nombre, aunque haga lo mismo, pero que simbolice que es otra
                return DataObtainingService.getRangesAndUses([categoryURI]);

                //Return all the promises, so the next function is called when all the promises are finished.
                //return $q.all(rangePromises);
                //console.log(propertyList);
            }).then(function (data) {





                    for (var i = 0; i < data.length; i++) {
                        var currentProperty = data[i];
                        var found = false;
                        var j = 0;
                        //We search for the handler in handlerList of that property
                        while (!found && j < handlers.length) {
                            var currentHandler = handlers[j];


                            if (currentProperty.key == currentHandler.propertyURI) {

                                //Attach ranges and uses.
                                currentHandler.ranges = currentProperty.value;
                                currentHandler.properties[0].ranges = currentProperty.value;

                                //For each property, its aggregated rangeUses is the sum of the uses of every range.
                                var rangeUsesCount = 0;
                                for(var k = 0; k < currentHandler.ranges.length; k++){
                                    var currentRange = currentHandler.ranges[k];
                                    rangeUsesCount+=currentRange.count;
                                }
                                currentHandler.useCount  =  rangeUsesCount;
                                currentHandler.properties[0].useCount = rangeUsesCount;
                                found = true;
                            }

                            j++;
                        }


                    }



                    //Category loaded, we return the array of PropertyHandler objects
                    return handlers;
                },
                function (errMessage) {
                    console.error("Error while retrieving mono-category data for " + categoryURI);
                    $q.reject(errMessage);
                }
            );

        };


    }]);
