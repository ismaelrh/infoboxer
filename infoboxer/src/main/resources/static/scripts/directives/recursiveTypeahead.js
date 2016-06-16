'use strict';

angular.module('infoboxer').
    /**
     Author: Ismael Rodríguez Hernández, 28/03/16.
     Directive to create a typeahead with tree view (hierarchy) in Infoboxer.
     */
    directive('treeTypeahead', ['$compile', '$templateRequest', '$filter', '$timeout', '$window', function ($compile, $templateRequest, $filter, $timeout, $window) {
        return {
            restrict: 'A',
            scope: {
                data: "=",          //Javascript object in recursive-form (with childs array). Source data
                ngModel: "=",       //ng-model of the input
                categoryModel: "=", //model of all the category object (with name and _id properties)
                alreadyEntered: "=" //List of already-entered categories, used to filter which ones show.
            },
            //In link we can access the scope.
            link: function (scope, elem, attrs) {

                //Set width to adjust to input
                scope.width = elem[0].clientWidth - 10;


                //Flatify-auxiliar is private function of flatify
                var flatifyaux = function (nodeArray, resultArray, currentDepth) {

                    //console.log(nodeArray);

                    for (var i = 0; i < nodeArray.length; i++) {
                        var current = nodeArray[i];
                        current.depth = currentDepth;
                        current.visibleByHit = true;
                        current.expanded = true;

                        //When we want the list collapsed
                        /*if(current.depth==0){
                         current.visibleByUnfold = true;
                         }
                         else{
                         current.visibleByUnfold = false;
                         }*/

                        //When we want the list unfolded
                        current.visibleByUnfold = true;


                        resultArray.push(current);

                        if (current.children && current.children.length > 0) {
                            current.hasChildren = true;


                            flatifyaux(current.children, resultArray, currentDepth + 1);
                        }
                        else {
                            current.hasChildren = false;
                        }
                    }
                    return resultArray;


                };

                //Flatify transforms a nested array of objects (with children properties) to a flat one with a depth param.
                var flatify = function (nodeArray) {
                    var arr = flatifyaux(nodeArray, [], 0);
                    for (var i = 0; i < arr.length; i++) {
                        delete arr[i].children;
                        arr[i].index = i;
                    }

                    return arr;

                };

                /**
                 * Unfolds a node, unfolding also the children if they have an 'expanded' property to true.
                 */
                var unfoldNode = function (nodeToUnfoldIndex) {

                    //console.log("Unfolding node " + nodeToUnfoldIndex);

                    var nodeToUnfold = scope.internalData[nodeToUnfoldIndex];
                    //nodeToUnfold.visibleByUnfold = true;
                    nodeToUnfold.expanded = true;

                    //Get its DIRECT children
                    var directChildrenIndexes = [];
                    var foundNodeAtSameDepth = false;
                    var currentIndex = nodeToUnfoldIndex + 1;
                    while (!foundNodeAtSameDepth && currentIndex < scope.internalData.length) {
                        var currentNode = scope.internalData[currentIndex];
                        if (currentNode.depth <= nodeToUnfold.depth) {
                            foundNodeAtSameDepth = true;
                        }
                        else if (currentNode.depth == nodeToUnfold.depth + 1) {
                            directChildrenIndexes.push(currentIndex);
                        }
                        currentIndex++;
                    }

                    //For each direct children, if it was expanded, do the same procedure
                    for (var i = 0; i < directChildrenIndexes.length; i++) {
                        var currentChild = scope.internalData[directChildrenIndexes[i]];
                        currentChild.visibleByUnfold = true; //Direct childs always visible
                        if (currentChild.expanded) {
                            unfoldNode(directChildrenIndexes[i]);
                        }

                    }


                };

                /** Folds a node, hidden all its children but saving their expanded state,
                 * so we can recover it when we unfold it.
                 */
                var foldNode = function (nodeToFoldIndex) {

                    //console.log("Folding node " + nodeToFoldIndex);
                    var foundNodeAtSameDepth = false;
                    var nodeToFold = scope.internalData[nodeToFoldIndex];

                    nodeToFold.expanded = false;
                    var currentIndex = nodeToFoldIndex + 1;


                    while (!foundNodeAtSameDepth && currentIndex < scope.internalData.length) {
                        var currentNode = scope.internalData[currentIndex];

                        //We set "visibleByUnfold" = false to all the nodes that are children of 'nodeToFold'
                        if (currentNode.depth > nodeToFold.depth) {
                            //Is more deep than initial node and we haven't found a node at same depth yet
                            //We conserv its expanded status

                            currentNode.visibleByUnfold = false;

                        }
                        else {
                               //Found one at same or higher level
                            foundNodeAtSameDepth = true;
                        }
                        currentIndex++;
                    }
                    console.dir(scope.internalData);
                };


                /**
                 * It expands all the parent nodes of the node with childIndex,
                 * and make them visible (visibleByHit and visibleByUnfold).
                 */
                var makeVisibleAndExpandFathers = function (childIndex) {

                    var child = scope.internalData[childIndex];
                    var childDepth = child.depth;
                    var nextParentDepth = childDepth - 1; //The next parent should have "child.depth -1" depth
                    for (var i = childIndex - 1; i >= 0; i--) {
                        //We go backwards until the first element, making visible by hit and by fold are the parents.

                        var currentNode = scope.internalData[i];
                        if (currentNode.depth == nextParentDepth) {
                            currentNode.expanded = true;
                            currentNode.visibleByHit = true;
                            currentNode.visibleByUnfold = true;
                            nextParentDepth--;
                        }
                    }
                };


                /**
                 * Returns the number of nodes that are currently visible.
                 */
                var countOfShowNodes = function () {

                    var visibleList = $filter('notIntroducedCategory')(scope.internalData, scope.alreadyEntered);
                    var count = 0;
                    for (var i = 0; i < visibleList.length; i++) {
                        if (visibleList[i].visibleByHit && visibleList[i].visibleByUnfold) {
                            count++;
                        }
                    }
                    return count;
                };

                /**
                 * Removes special characters.
                 */
                var removeAccents = function (cadena) {
                    cadena = cadena.toLowerCase();
                    cadena = cadena.replace("á", "a");
                    cadena = cadena.replace("à", "a");
                    cadena = cadena.replace("é", "e");
                    cadena = cadena.replace("è", "e");
                    cadena = cadena.replace("í", "i");
                    cadena = cadena.replace("ì", "i");
                    cadena = cadena.replace("ó", "o");
                    cadena = cadena.replace("ò", "o");
                    cadena = cadena.replace("ú", "u");
                    cadena = cadena.replace("ù", "u");
                    cadena = cadena.replace("ü", "u");
                    return cadena;
                };

                /**
                 * Returns true if the node has children that are currently visible by hit.
                 * Used to hide the ">" fold/unfold button when no children is hit.
                 */
                scope.hasVisibleByHitChildren = function (node) {
                    var has = false;
                    var stop = false;
                    for (var i = node.index + 1; i < scope.internalData.length && !stop; i++) {
                        var currentNode = scope.internalData[i];
                        if (currentNode.depth <= node.depth) {
                            stop = true;
                        }
                        else if (currentNode.depth == node.depth + 1) {
                            if (currentNode.visibleByHit) {
                                has = true;
                                stop = true;
                            }
                        }
                    }
                    return has;

                };


                /**
                 * Returns true if the node has any children that is unique (not repeated in other lists), although
                 * it is hidden because it is folded..
                 * Used to hide the ">" fold/unfold button when no children is hit.
                 */
                scope.hasUniqueChildren = function (node) {

                    var allUnique = $filter('notIntroducedCategory')(scope.internalData, scope.alreadyEntered);

                    //Get its DIRECT children
                    var directChildren = [];
                    var foundNodeAtSameDepth = false;
                    var currentIndex = node.index + 1;
                    while (!foundNodeAtSameDepth && currentIndex < scope.internalData.length) {
                        var currentNode = scope.internalData[currentIndex];
                        if (currentNode.depth <= node.depth) {
                            foundNodeAtSameDepth = true;
                        }
                        else if (currentNode.depth == node.depth + 1) {
                            directChildren.push(currentNode);
                        }
                        currentIndex++;
                    }


                    //For every direct children, check if it is in allUniqueList
                    var isInUniqueList = false;
                    for (var k = 0; k < directChildren.length && !isInUniqueList; k++) {
                        for (var j = 0; j < allUnique.length && !isInUniqueList; j++) {
                            if (allUnique[j].name == directChildren[k].name) {
                                isInUniqueList = true;
                            }
                        }
                    }


                    return isInUniqueList;

                };


                /**
                 * Folds or unfolds a node depending on it's state.
                 * When it opens, it restores the previously fold status of its children.
                 * It MUST be called on ng-mousedown so it fires before "blur" event.
                 */
                scope.toggleChildren = function (node, id) {


                    if (node.expanded) {
                        foldNode(node.index);
                    }
                    else {
                        unfoldNode(node.index);
                    }

                    //Last action that provocated a blur was a push, not a focus outside.
                    scope.lastAction = "push";


                };


                //Used to control unfocus action
                scope.lastAction = undefined;


                $templateRequest("views/recursiveTemplate.html").then(function (html) {


                    //Init data in 200ms, so scope.data is fully populated
                    $timeout(function () {


                        //Copy the data so we can modify it internally
                        scope.internalData = [];
                        angular.copy(scope.data, scope.internalData);

                        //Flatify the data so we can work with it easily :)
                        scope.internalData = flatify(scope.internalData);


                        //Generate and compile template
                        var template = angular.element(html);
                        elem.after(template);
                        $compile(template)(scope);


                        //Used to handle selection with keys
                        elem.bind("keydown", function ($event) {

                            if ($event.keyCode == 38 && scope.active > 0) { // arrow up
                                scope.active--;
                                scope.$digest()
                            } else if ($event.keyCode == 40 && scope.active < countOfShowNodes() - 1) { // arrow down
                                scope.active++;
                                scope.$digest()
                            } else if ($event.keyCode == 13) { // enter
                                var visibleList = $filter('showFilter')(scope.internalData);
                                visibleList = $filter('notIntroducedCategory')(visibleList, scope.alreadyEntered);
                                scope.$apply(function () {
                                    scope.lastAction = "enter"; //Important, so we do not enter an $apply inside $apply error.
                                    scope.click(visibleList[scope.active])

                                })
                            }

                        });


                        //Current active element
                        scope.active = 0;

                        //When mouse hovers, set to active
                        scope.mouseenter = function (activeIndex) {
                            scope.active = activeIndex;
                        };


                        //Handle click action. Sets categoryModel values and blurs if it is needed to.
                        scope.click = function (node) {

                            // scope.model = node.name;
                            scope.categoryModel.name = node.name;
                            scope.categoryModel._id = node._id;
                            scope.focused = false;

                            //Only if enter was pressed, call blur. (At default, it focus again).
                            if (scope.lastAction == "enter" || scope.lastAction == "hit") {
                                elem[0].blur();
                            }


                        };


                        /** Called when the input is out-focused.
                         * Four cases:
                         * Click on a list element: focus again on input.
                         * Enter pressed: hide list, remove focus from input.
                         * Hit while typing: hide list, remove focus from input.
                         * Click outside: hide list
                         */
                        elem.bind('blur', function () {

                            if (scope.lastAction == "push") {
                                //Nothing
                                //Focus on input
                                elem[0].focus();
                            }
                            else if (scope.lastAction == "enter") {
                                //If enter, do nothing.
                            }
                            else if (scope.lastAction == "hit") {
                                //If hit, do nothing
                            }
                            else {
                                //Focused outside -> Hide
                                scope.$apply(function () {
                                    scope.focused = false;
                                })

                            }

                            //Restore value of lastActions
                            scope.lastAction = undefined;


                        });


                        //Opens the list 100ms after focusing (If no 100ms are waited,then it does not open)
                        elem.bind('focus', function () {


                            $timeout(function () {
                                scope.focused = true;

                            }, 100)


                        });

                        //Watchs for changes in the input text and updates the list
                        scope.$watch('ngModel', function (input) {


                            var inputText = "";
                            if (input == undefined || input == null) {
                                inputText = "";
                            }
                            else {
                                inputText = input.toString();
                            }
                            inputText = removeAccents(inputText.toLowerCase());

                            var alreadySelected = 0;

                            //First, we get the indexes of the nodes that hit.
                            //Also, set visible by hit true or false whether it is hit or not.
                            var nodesThatHit = [];
                            //Todo: name is not generic, used only for infoboxer
                            for (var i = 0; i < scope.internalData.length; i++) {
                                var name = removeAccents(scope.internalData[i].name.toLowerCase());

                                if (name.indexOf(inputText) > -1) {
                                    scope.internalData[i].visibleByHit = true;
                                    scope.internalData[i].visibleByUnfold = true;
                                    nodesThatHit.push(scope.internalData[i]);

                                    if (alreadySelected == 0) {
                                        scope.active = i;
                                        alreadySelected++;
                                    }
                                }
                                else {
                                    scope.internalData[i].visibleByHit = false;
                                }
                            }

                            //Second, for every node that hits, we expand and make visible their fathers.
                            for (var j = 0; j < nodesThatHit.length; j++) {

                                if (inputText.length > 0) {
                                    makeVisibleAndExpandFathers(nodesThatHit[j].index);
                                }

                            }


                            //Third, make as active the first one
                            var visibleList = $filter('showFilter')(scope.internalData);
                            visibleList = $filter('notIntroducedCategory')(visibleList, scope.alreadyEntered);

                            if (nodesThatHit.length > 0) { //If some node hits, get its index from visible list and mark it as active
                                for (var k = 0; k < visibleList.length; k++) {
                                    if (visibleList[k].index == nodesThatHit[0].index) {
                                        scope.active = k;
                                        break;
                                    }
                                }
                            }

                            //Forth, check if text in input is same as the only element that matches. If so, click
                            if (nodesThatHit.length == 1 && removeAccents(nodesThatHit[0].name.toLowerCase()) == inputText) {
                                scope.lastAction = "hit";
                                scope.click(nodesThatHit[0]);

                            }
                            else{
                                //No hits -> Undefined _id so it alerts when trying to load data.
                                scope.categoryModel._id = undefined;
                            }


                        })


                    }, 200);


                });


            }
        }
    }])

    //Filters only items that are both visibleByHit and visibleByUnfold
    .filter('showFilter', function () {
        return function (items) {
            var filtered = [];
            angular.forEach(items, function (item) {
                if (item.visibleByHit && item.visibleByUnfold) {
                    filtered.push(item);
                }
            });
            return filtered;
        };
    })
    //Filters only items that are not repeated on other lists, or have children.
    .filter('notIntroducedCategory', function () {
        return function (items, insertedCategories) {
            var filtered = [];
            angular.forEach(items, function (item) {
                var contained = false;

                for (var i = 0; i < insertedCategories.length; i++) {
                    if (insertedCategories[i].name == item.name) {
                        contained = true;
                    }

                }
                if (!contained ||  item.hasChildren) {

                    filtered.push(item);

                }

            });

            return filtered;

        };
    });
