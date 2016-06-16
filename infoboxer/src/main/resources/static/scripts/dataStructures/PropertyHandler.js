'use strict';

function PropertyHandler() {

    this.propertyURI = undefined; //Property URI
    this.propertyLabel = undefined;
    this.categoriesCount = undefined; //Count of categories instances
    this.instanceCount = undefined; //Count of instances that manifest this prop
    this.useCount = undefined; //Count of uses of this prop
    this.popularity = undefined; //Popularity index
    this.ranges = [];
    this.values = [];
    this.properties = [];

}

//Return an array of categories URIs of the properties contained in PropertyHandler.
PropertyHandler.prototype.getCategoriesURIs = function () {
    var catNumber = this.properties.length;
    var categoryList = [];
    for (var i = 0; i < catNumber; i++) {
        categoryList.push(this.properties[i].category.categoryURI);
    }
    return categoryList;
};


PropertyHandler.prototype.getCategoriesURIsList = function () {
    var catNumber = this.properties.length;
    var categoryList = this.properties[0].category.categoryURI;

    for (var i = 1; i < catNumber; i++) {
        categoryList += ", " + this.properties[i].category.categoryURI;
    }
    return categoryList;
};


PropertyHandler.prototype.getPopularRanges = function () {
    var result = [];
    for (var i = 0; i < this.ranges.length && i < 3; i++) {
        result.push({
            range: this.ranges[i]._id,
            percentage: this.ranges[i].count * 100 / this.useCount
        });
    }
    return result;
};

//Adds a new value, either with the given text or empty string
PropertyHandler.prototype.addValue = function (value) {
    if (!value) {
        value = "";
    }
    this.values.push(new Value(value, "XMLSchema#String"));

};

//Removes value from value array by index
PropertyHandler.prototype.removeValue = function (index) {
    if (index > -1) {
        this.values.splice(index, 1);
    }
};
