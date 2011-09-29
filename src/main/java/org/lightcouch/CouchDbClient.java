package org.lightcouch;

import static org.lightcouch.CouchDbUtil.*;
import static org.lightcouch.URIBuilder.*;

import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpResponse;

import com.google.gson.JsonObject;

/**
 * <p>Presents a client to a CouchDB database instanse.
 * <p>This is the main class to use to gain access to the various APIs defined by this client.
 * 
 * <h3>Usage Example:</h3> 
 * <p>Instantiating an instance of this class requires configuration options to be supplied.
 * Properties files may be used for this purpose. See overloaded constructors for available options.
 * <p>A typical example for creating an instance is by preparing a properties file named 
 * <tt>couchdb.properties</tt> and placing it in your application classpath:
 * 
 * <pre>
 * couchdb.name=my-db
 * couchdb.createdb.if-not-exist=true
 * couchdb.protocol=http
 * couchdb.host=127.0.0.1
 * couchdb.port=5984
 * couchdb.username=myuser
 * couchdb.password=secret
 * </pre>
 * 
 * <p>Then construct a new instance using the default constructor: 
 * <pre>
 * CouchDbClient dbClient = new CouchDbClient(); // looks for <tt>classpath:couchdb.properties</tt>
 * // access the API here
 * </pre>
 * <p>Multiple client instances could be created to handle multiple database instances simultaneously in a thread-safe manner, 
 * typically one client for each database. 
 * 
 * <p>A client instance provides access to various APIs, accessible under four locations or contexts.
 * <p>Document APIs are available directly under this instance:
 * <pre>
 *  Foo foo = dbClient.find(Foo.class, "some-id");
 * </pre>
 * 
 * <p>Design documents API under the context <tt>design()</tt> {@link CouchDbDesign} contains usage example.
 * 
 * <p>View APIs under the context <tt>view()</tt> {@link View} contains usage examples.
 * 
 * <p>Replication APIs under two contexts: <tt>replication()</tt> and <tt>replicator()</tt>, 
 * the latters supports the replicator database introduced with CouchDB v 1.1.0 
 * {@link Replication} and {@link Replicator} provide usage examples.
 * 
 * <p>Database APIs under the context <tt>context()</tt>
 * 
 * <p>After completing usage of this client, it might be useful to shutdown it's 
 * underlying connection manager to ensure proper release of resources: 
 * <tt>dbClient.shutdown()</tt>
 * 
 * @author Ahmed Yehia
 *
 */
public final class CouchDbClient extends CouchDbClientBase {

	// -------------------------------------------------------------------------- Constructors
	/**
	 * Constructs a new instance of this class, expects a configuration file named 
	 * <code>couchdb.properties</code> to be available in your application classpath.
	 */
	public CouchDbClient() {
		super();
	}
	
	/**
	 * Constructs a new instance of this class.
	 * @param configFileName The configuration file name.
	 */
	public CouchDbClient(String configFileName) {
		super(new CouchDbConfig(configFileName));
	}
	
	/**
	 * Constructs a new instance of this class.
	 * @param dbName The database name.
	 * @param createDbIfNotExist To create a new database if it does not already exist.
	 * @param protocol The protocol to use (i.e http or https)
	 * @param host The database host address
	 * @param port The database listening port
	 * @param username The Username credential
	 * @param password The Password credential
	 */
	public CouchDbClient(String dbName, boolean createDbIfNotExist, 
			String protocol, String host, int port, String username, String password) { 
		super(new CouchDbConfig(dbName, createDbIfNotExist, protocol, host, port, username, password));
	}
	
	private CouchDbContext context;
	private CouchDbDesign design;
	
	{ 
		context = new CouchDbContext(this, getConfig()); 
		design = new CouchDbDesign(this);
	}
	
	/**
	 * Provides access to the database APIs.
	 */
	public CouchDbContext context() {
		return context;
	}
	
	/**
	 * Provides access to the database design documents API.
	 */
	public CouchDbDesign design() {
		return design;
	}
	
	/**
	 * Provides access to the View APIs.
	 */
	public View view(String viewId) {
		return new View(this, viewId);
	}
	
	/**
	 * Provides access to the replication APIs.
	 */
	public Replication replication() {
		return new Replication(this);
	}
	
	/**
	 * Provides access to the replicator database APIs.
	 */
	public Replicator replicator() {
		return new Replicator(this);
	}
	
	/**
	 * Finds an Object of the specified type.
	 * @param <T> Object type.
	 * @param classType The class of type T.
	 * @param id The document id.
	 * @return An object of type T.
	 * @throws NoDocumentException If the document is not found in the database.
	 */
	public <T> T find(Class<T> classType, String id) {
		assertNotEmpty(classType, "Class Type");
		assertNotEmpty(id, "id");
		return get(builder(getDBUri()).path(id).build(), classType);
	}
	
	/**
	 * Finds an Object of the specified type.
	 * @param <T> Object type.
	 * @param classType The class of type T.
	 * @param id The document id to get.
	 * @param rev The document revision.
	 * @return An object of type T.
	 * @throws NoDocumentException If the document is not found in the database.
	 */
	public <T> T find(Class<T> classType, String id, String rev) {
		assertNotEmpty(classType, "Class Type");
		assertNotEmpty(id, "id");
		assertNotEmpty(id, "rev");
		URI uri = builder(getDBUri()).path(id).query("rev", rev).build();
		return get(uri, classType);
	}
	
	/**
	 * <p>Finds a document and returns the result as an {@link InputStream}.</p>
	 * The stream should be properly closed after usage, as to avoid connection leaks.
	 * @param id The document id.
	 * @return The result of the request as an {@link InputStream}
	 * @throws NoDocumentException If the document is not found in the database.
	 */
	public InputStream find(String id) {
		assertNotEmpty(id, "id");
		return get(builder(getDBUri()).path(id).build());
	}
	
	/**
	 * <p>Finds a document given an id and revision, returns the result as {@link InputStream}.</p>
	 * The stream should be properly closed after usage, as to avoid connection leaks.
	 * @param id The document id.
	 * @return The result of the request as an {@link InputStream}
	 * @throws NoDocumentException If the document is not found in the database.
	 */
	public InputStream find(String id, String rev) {
		assertNotEmpty(id, "id");
		assertNotEmpty(rev, "rev");
		return get(builder(getDBUri()).path(id).query("rev", rev).build());
	}
	
	/**
	 * Checks if the database contains a document given an id.
	 * @param id The document id.
	 * @return true If the document is found, false otherwise.
	 */
	public boolean contains(String id) {
		assertNotEmpty(id, "id");
		InputStream instream = null;
		try {
			instream = find(id);
		} catch (NoDocumentException e) {
			return false;
		} finally {
			close(instream);
		}
		return true;
	}
	
	/**
	 * Saves an object in the database.
	 * @param object The object to save
	 * @throws DocumentConflictException If a conflict is detected during the save.
	 * @return {@link Response}
	 */
	public Response save(Object object) {
		return put(getDBUri(), object, true);
	}
	
	/**
	 * Saves the given object in a batch request.
	 * @param object The object to save.
	 */
	public void batch(Object object) {
		HttpResponse response = null;
		try { 
			URI uri = builder(getDBUri()).query("batch", "ok").build();
			response = post(uri, getGson().toJson(object));
		} finally {
			close(response);
		}
	}
	
	/**
	 * Updates an object in the database, the object must have the correct id and revision values.
	 * @param object The object to update
	 * @throws DocumentConflictException If a conflict is detected during the update.
	 * @return {@link Response}
	 */
	public Response update(Object object) {
		return put(getDBUri(), object, false);
	}
	
	/**
	 * Removes an object from the database, the object must have the correct id and revision values.
	 * @param object The object to remove
	 * @throws NoDocumentException If the document could not be found in the database.
	 * @return {@link Response}
	 */
	public Response remove(Object object) {
		assertNotEmpty(object, "Entity");
		JsonObject jsonObject = objectToJson(getGson(), object);
		String id = getElement(jsonObject, "_id");
		String rev = getElement(jsonObject, "_rev");
		return remove(id, rev);
	}
	
	/**
	 * Removes a document from the database, given both an id and revision values.
	 * @param id The document id
	 * @param rev The document revision
	 * @throws NoDocumentException If the document could not be found in the database.
	 * @return {@link Response}
	 */
	public Response remove(String id, String rev) {
		assertNotEmpty(id, "id");
		assertNotEmpty(rev, "revision");
		return delete(builder(getDBUri()).path(id).query("rev", rev).build());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		super.shutdown();
	}
}