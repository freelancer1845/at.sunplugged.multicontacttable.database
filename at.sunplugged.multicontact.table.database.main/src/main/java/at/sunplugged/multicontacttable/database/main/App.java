package at.sunplugged.multicontacttable.database.main;

import java.io.File;
import java.io.IOException;
import java.util.stream.StreamSupport;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, InterruptedException
    {
    	Database database = DatabaseBuilder.open(new File("C:\\Users\\jasch\\SunpluggedJob\\Multicontakttable\\VOC1200Labor\\save_old\\processdaten.mdb"));
    	
    	System.out.println("### Table Names ###");
    	database.getTableNames().forEach(table -> {
    		System.out.println("#### TABLE: ####");
    		System.out.println(table);
    		try {
				database.getTable(table).getColumns().forEach(column -> System.out.print(column.getName() + "| "));
			} catch (IOException e) {
				e.printStackTrace();
			}
    	});
    	for (int i = 0; i < 400; i++) {
    		database.getTable("LogModul").addRow(3052 + i, "Test" + i);
    	}
    
//    	System.out.println("### Table Names End###");
		// Table logVTable = database.getTable("LogV");
		// int maxID = StreamSupport.stream(logVTable.spliterator(), false).mapToInt(row
		// -> row.getInt("ID")).max().getAsInt();
		// System.out.println("Max ID: " + maxID);
    	//    	database.getTable("LogV").forEach(row -> System.out.println(row.getInt("ID") + "| " + row.getShort("Messreihe") + "| " + row.getShort("Segement") + "| " + row.getShort("MesswertV")));
//       CursorBuilder.findRow(database.getTable("LogV"), rowPattern)
    }
}
