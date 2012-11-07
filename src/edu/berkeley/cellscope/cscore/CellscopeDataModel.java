package edu.berkeley.cellscope.cscore;


public class CellscopeDataModel {
	public CellscopeDataModel() {
		 // this uses h2 by default but change to match your database
        String databaseUrl = "jdbc:h2:mem:account";
        // create a connection source to our database
        
        /*
        
        ConnectionSource connectionSource = 
            new AndroidConnectionSource(databaseUrl);

        // instantiate the dao
        Dao<CSPicture, String> accountDao =
            DaoManager.createDao(connectionSource, CSPicture.class);

        // if you need to create the 'accounts' table make this call
        TableUtils.createTable(connectionSource, CSPicture.class);
	
	*/
	
	}
}
