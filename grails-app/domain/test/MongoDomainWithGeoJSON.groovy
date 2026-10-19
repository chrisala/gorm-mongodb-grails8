package test

import org.bson.types.ObjectId

class MongoDomainWithGeoJSON {


    ObjectId id

    String name
    String description
    Map geoJson

    static constraints = {
    }
}
