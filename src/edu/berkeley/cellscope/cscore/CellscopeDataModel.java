package edu.berkeley.cellscope.cscore;

import com.j256.ormlite.android.AndroidConnectionSource;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class CellscopeDataModel {
	public CellscopeDataModel() {
		 // this uses h2 by default but change to match your database
        String databaseUrl = "jdbc:h2:mem:account";
        // create a connection source to our database
        ConnectionSource connectionSource = 
            new AndroidConnectionSource(databaseUrl);

        // instantiate the dao
        Dao<CSPicture, String> accountDao =
            DaoManager.createDao(connectionSource, CSPicture.class);

        // if you need to create the 'accounts' table make this call
        TableUtils.createTable(connectionSource, CSPicture.class);
	}
}
