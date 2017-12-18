package at.sunplugged.multicontacttable.database.services;

import java.io.IOException;
import java.util.List;

import at.sunplugged.multicontacttable.database.model.Result;
import javafx.collections.ObservableList;

public interface DatabaseService {

	public interface Messreihe {
		
		Integer getIndex();
		
		List<Integer> getSegmente();
		
		List<Integer> getRValues();
		
		List<Integer> getVValues();
		
	}

	ObservableList<Result> getResultOberservableList();
	
	void loadNewValues();
	
	List<Messreihe> getValues(Result result) throws IOException;
	
}
