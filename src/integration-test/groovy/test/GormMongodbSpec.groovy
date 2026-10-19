package test


import grails.testing.mixin.integration.Integration
import org.bson.BsonArray
import org.bson.BsonDouble
import org.bson.BsonString
import org.bson.Document
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Attempts to reproduce errors when using Maps with keys named "properties" in GORM for MongoDB.
 */
@Integration
class GormMongodbSpec extends Specification {

    @Shared
    @AutoCleanup
    final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:latest"))

    void "Maps with keys named properties should be treated as Map keys, not the groovy getProperties() method when binding/validating"() {

        setup:
        Map geojson = [type: "Feature", geometry: [type: "Point", coordinates: [100.0, 0.0]], properties: [name: "Test"]]

        when:
        MongoDomainWithGeoJSON.withTransaction {
            new MongoDomainWithGeoJSON(name: "Test", description: "Test description", geoJson: geojson).save(flush: true, failOnError:true)
        }

        then:
        noExceptionThrown()

        when:
        def retrieved = MongoDomainWithGeoJSON.findByName("Test")

        then:
        retrieved?.geoJson?.getOrDefault('properties', [:])?['name'] == "Test"
    }

    void "Keys named properties should be treated as Map keys, not the groovy getProperties() method when decoding a Map"() {

        setup:
        Map geojson = [type: "Feature", geometry: [type: "Point", coordinates: [100.0, 0.0]], properties: [name: "Test"]]

        when:
        def mongoMappingTest = new MongoDomainWithGeoJSON(name: "Test", description: "Test description").save(flush: true, failOnError:true)
        mongoMappingTest.geoJson = geojson
        mongoMappingTest.markDirty("geoJson") // Not required in Grails 7
        mongoMappingTest.save(flush: true, failOnError:true)


        then:
        noExceptionThrown()

        when:
        def retrieved = MongoDomainWithGeoJSON.findByName("Test")

        then:
        retrieved?.geoJson?.getOrDefault('properties', [:])?['name'] == "Test"
    }

    void "Keys named properties should be treated as Map keys, not the groovy getProperties() method when mapping a queried Document"() {
        setup:
        Document doc = new Document()
        doc.put("name", "Test")
        doc.put("description", "Test description")
        Document geoJson = new Document()
        geoJson.put("type", "Feature")
        Document geometry = new Document()
        geometry.put("type", new BsonString("Point"))
        geometry.put("coordinates", new BsonArray([new BsonDouble(100.0), new BsonDouble(0.0)]))
        geoJson.put("geometry", geometry)
        Document properties = new Document()
        properties.put("name", new BsonString("Test"))
        geoJson.put("properties", properties)
        doc.put("geoJson", geoJson)
        MongoDomainWithGeoJSON.collection.insertOne(doc)

        when:
        def retrieved = MongoDomainWithGeoJSON.findByName("Test")

        then:
        retrieved?.geoJson?.getOrDefault('properties', [:])?['name'] == "Test"
    }
}
