package org.openshift.mlbparks.mongo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

@Named
@ApplicationScoped
public class DBConnection {

	private DB mongoDB;

	public DBConnection() {
		super();
	}

	@PostConstruct
	public void afterCreate() {
		Map<String, String> envMap = System.getenv();
		Iterator it = envMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
		        System.out.println(pair.getKey() + " = " + pair.getValue());
		        //it.remove(); // avoids a ConcurrentModificationException
		}
		
		//String mongoHost = (System.getenv("MONGODB_SERVICE_HOST") == null) ? "127.0.0.1" : System.getenv("MONGODB_SERVICE_HOST");
		//String mongoPort = (System.getenv("MONGODB_SERVICE_PORT") == null) ? "27017" : System.getenv("MONGODB_SERVICE_PORT"); 
		String mongoUser = (System.getenv("MONGODB_USER")== null) ? "mlbparks" : System.getenv("MONGODB_USER");
		String mongoPassword = (System.getenv("MONGODB_PASSWORD") == null) ? "mlbparks" : System.getenv("MONGODB_PASSWORD");
		String mongoDBName = (System.getenv("MONGODB_DATABASE") == null) ? "mlbparks" : System.getenv("MONGODB_DATABASE");
		// Check if we are using a mongoDB template or mongodb RHEL 7 image
		/*
		if (mongoHost == null) {
			mongoHost = System.getenv("MONGODB_24_RHEL7_SERVICE_HOST");
		} 
		if (mongoPort == null) {
			mongoPort = System.getenv("MONGODB_24_RHEL7_SERVICE_PORT");
		}
		*/
		//int port = Integer.decode(mongoPort);
		
		Mongo mongo = null;
		//try {
			mongo = new Mongo(getServers(3));
			
			//mongo = new Mongo(mongoHost, port);
			mongo.setReadPreference(ReadPreference.primaryPreferred());
			System.out.println("Connected to database");
		//} catch (UnknownHostException e) {
		//	System.out.println("Couldn't connect to MongoDB: " + e.getMessage() + " :: " + e.getClass());
		//}

		mongoDB = mongo.getDB(mongoDBName);

		if (mongoDB.authenticate(mongoUser, mongoPassword.toCharArray()) == false) {
			System.out.println("Failed to authenticate DB ");
		}

		this.initDatabase(mongoDB);

	}

	public DB getDB() {
		return mongoDB;
	}
	
	private List<ServerAddress> getServers(int count) {
		List<ServerAddress> servers = new ArrayList<ServerAddress>();
		try {
		
			for (int i=1;i<=count;i++) {
				String mongohost = System.getenv("REPLICA_"+ i +"_SERVICE_HOST");
				String mongoport = System.getenv("REPLICA_"+ i +"_SERVICE_PORT");
				System.out.println(mongohost);
				System.out.println(mongoport);
				if (null != mongohost && null != mongoport) {
					ServerAddress address = new ServerAddress(mongohost, Integer.decode(mongoport));
					servers.add(address);
				}
			}
			if(servers.isEmpty()) {
				String mongohost = (System.getenv("MONGODB_SERVICE_HOST") == null) ? "127.0.0.1" : System.getenv("MONGODB_SERVICE_HOST");
				String mongoport = (System.getenv("MONGODB_SERVICE_PORT") == null) ? "27017" : System.getenv("MONGODB_SERVICE_PORT"); 
				if (null != mongohost && null != mongoport) {
					ServerAddress address = new ServerAddress(mongohost, Integer.decode(mongoport));
					servers.add(address);
				}
			}
		} catch (UnknownHostException e) {
			System.out.println("Couldn't connect to MongoDB: " + e.getMessage() + " :: " + e.getClass());
		}
		return servers;
	}

	private void initDatabase(DB mongoDB) {
		DBCollection parkListCollection = mongoDB.getCollection("teams");
		int teamsImported = 0;
		if (parkListCollection.count() < 1) {
			System.out.println("The database is empty.  We need to populate it");
			try {
				String currentLine = new String();
				URL jsonFile = new URL(
						"https://raw.githubusercontent.com/gshipley/openshift3mlbparks/master/mlbparks.json");
				BufferedReader in = new BufferedReader(new InputStreamReader(jsonFile.openStream()));
				while ((currentLine = in.readLine()) != null) {
					parkListCollection.insert((DBObject) JSON.parse(currentLine.toString()));
					teamsImported++;
				}
				System.out.println("Successfully imported " + teamsImported + " teams.");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
